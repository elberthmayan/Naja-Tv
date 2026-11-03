package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Tela "Temas": grade de imagens da TV para aplicar como wallpaper sem ADB.
 *
 * As imagens vem das pastas publicas de midia (Download, DCIM, Pictures) e de qualquer volume
 * montado em /storage; a copia escolhida vai para files/wallpapers/ e vira o wallpaper_choice.
 */
public final class ThemePickerActivity extends Activity {

    private static final int PICK = 9211;
    private static final int READ_IMAGES = 9212;

    private Adapter mAdapter;
    private SharedPreferences mPrefs;
    private TextView mEmpty;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        int dp4 = dp(4), dp6 = dp(6), dp12 = dp(12), dp16 = dp(16), dp20 = dp(20), dp32 = dp(32);

        // Root: fundo escuro.
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF101419);
        root.setPadding(dp32, dp32, dp32, dp32);
        setContentView(root);

        mPrefs = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);

        // Header: titulo + botoes.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.bottomMargin = dp20;
        header.setLayoutParams(headerLp);
        root.addView(header);

        TextView title = new TextView(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setText(R.string.theme_picker_title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        header.addView(title);

        TextView btnPick = makeBtn(getString(R.string.theme_pick_button), dp20);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        btnLp.leftMargin = dp16;
        btnPick.setLayoutParams(btnLp);
        btnPick.setOnClickListener(v -> pickFromStorage());
        header.addView(btnPick);

        TextView btnRemove = makeBtn(getString(R.string.theme_remove_button), dp20);
        LinearLayout.LayoutParams btn2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        btn2Lp.leftMargin = dp12;
        btnRemove.setLayoutParams(btn2Lp);
        btnRemove.setOnClickListener(v -> removeWallpaper());
        header.addView(btnRemove);

        // Hint.
        TextView hint = new TextView(this);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.bottomMargin = dp16;
        hint.setLayoutParams(hintLp);
        hint.setText(R.string.theme_picker_hint);
        hint.setTextColor(0xFFACB8C1);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        root.addView(hint);

        // Vazio: nenhuma imagem encontrada.
        mEmpty = new TextView(this);
        mEmpty.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mEmpty.setText(R.string.theme_no_images);
        mEmpty.setTextColor(0xFFACB8C1);
        mEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mEmpty.setGravity(Gravity.CENTER);
        mEmpty.setPadding(0, dp(40), 0, 0);
        mEmpty.setVisibility(View.GONE);
        root.addView(mEmpty);

        // Grade.
        RecyclerView grid = new RecyclerView(this);
        grid.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        grid.setLayoutManager(new GridLayoutManager(this, 4));
        grid.setClipToPadding(false);
        grid.setFocusable(true);
        mAdapter = new Adapter();
        grid.setAdapter(mAdapter);
        root.addView(grid);

        askForImages();
        scan();
    }

    /** Cria um TextView estilizado como botao focavel (herda o estilo da TV). */
    private TextView makeBtn(String text, int hPad) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(hPad, 0, hPad, 0);
        tv.setGravity(Gravity.CENTER);
        tv.setFocusable(true);
        tv.setClickable(true);
        tv.setBackground(getDrawable(R.drawable.settings_choice_bg));
        return tv;
    }

    /** O seletor de documentos do Android: o usuario escolhe a imagem de onde quiser. */
    private void pickFromStorage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        try {
            startActivityForResult(i, PICK);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * As imagens da TV (Download, DCIM, Pictures, um pen drive) so podem ser lidas com a permissao de
     * midia. Ela e pedida so aqui, quando a tela de Temas abre; launcher em si nunca a pede.
     */
    private void askForImages() {
        String permission = android.os.Build.VERSION.SDK_INT >= 33
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{permission}, READ_IMAGES);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == READ_IMAGES) scan();
    }

    /** Volta ao fundo preto: sem imagem escolhida. */
    private void removeWallpaper() {
        mPrefs.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
        bump();
        Toast.makeText(this, R.string.theme_wallpaper_removed, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK && res == RESULT_OK && data != null && data.getData() != null)
            applyUri(data.getData());
    }

    /** Copia a imagem escolhida para files/wallpapers/ e a aponta como wallpaper. */
    private void applyUri(final Uri uri) {
        new Thread(() -> {
            final Bitmap bmp = decodeUri(uri, 1920);
            if (bmp == null) {
                runOnUiThread(() -> Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show());
                return;
            }
            File dir = Config.dir(this);
            if (dir == null) {
                bmp.recycle();
                return;
            }
            File wpDir = new File(dir, "wallpapers");
            if (!wpDir.isDirectory()) wpDir.mkdirs();
            final File saved = new File(wpDir, "custom.jpg");
            try (FileOutputStream fos = new FileOutputStream(saved)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 88, fos);
            } catch (Exception e) {
                saved.delete();
                bmp.recycle();
                return;
            }
            bmp.recycle();
            runOnUiThread(() -> {
                mPrefs.edit().putString(Config.WALLPAPER_CHOICE_KEY, saved.getName()).apply();
                bump();
                scan();
                Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    /** Aplica uma imagem ja presente na TV: a que estiver na pasta de wallpapers vira o wallpaper. */
    private void applyFile(final File file) {
        File dir = Config.dir(this);
        if (dir != null) {
            File wpDir = new File(dir, "wallpapers");
            if (file.getParentFile() != null && file.getParentFile().equals(wpDir)) {
                mPrefs.edit().putString(Config.WALLPAPER_CHOICE_KEY, file.getName()).apply();
                bump();
                Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        applyUri(Uri.fromFile(file));
    }

    /** Avisa o launcher em execucao de que a escolha mudou. */
    private void bump() {
        mPrefs.edit().putLong(Config.SETTINGS_REVISION_KEY,
                mPrefs.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    /** Procura imagens nas pastas publicas e em cada volume montado. */
    private void scan() {
        new Thread(() -> {
            final List<File> list = new ArrayList<>();
            addDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), list);
            addDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), list);
            addDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), list);
            File dir = Config.dir(this);
            if (dir != null) addDir(new File(dir, "wallpapers"), list);
            try {
                File[] vols = new File("/storage").listFiles();
                if (vols != null) for (File v : vols) {
                    if ("emulated".equals(v.getName()) || "self".equals(v.getName())) continue;
                    addDir(v, list);
                    addDir(new File(v, "DCIM"), list);
                    addDir(new File(v, "Download"), list);
                }
            } catch (Exception ignored) { }
            runOnUiThread(() -> {
                mEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                mAdapter.set(list);
            });
        }).start();
    }

    /** Junta as imagens de uma pasta, das mais recentes para as mais antigas. */
    private static void addDir(File dir, List<File> out) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File f : files) if (f.isFile() && isImg(f) && !out.contains(f)) out.add(f);
    }

    private static boolean isImg(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
    }

    private static Bitmap decode(File f, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            if (o.outWidth <= 0) return null;
            o.inSampleSize = sample(o.outWidth, o.outHeight, max);
            o.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(f.getPath(), o);
        } catch (Throwable e) {
            return null;
        }
    }

    private Bitmap decodeUri(Uri uri, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, o);
            }
            if (o.outWidth <= 0) return null;
            o.inSampleSize = sample(o.outWidth, o.outHeight, max);
            o.inJustDecodeBounds = false;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                return BitmapFactory.decodeStream(in, null, o);
            }
        } catch (Throwable e) {
            return null;
        }
    }

    /** Metade maior do que o limite: a TV decodifica few megapixels, nao o arquivo original. */
    private static int sample(int w, int h, int max) {
        int s = 1;
        while (w / s > max && h / s > max) s *= 2;
        return s;
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    // --- Grade ---

    private final class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<File> data = new ArrayList<>();

        void set(List<File> list) {
            data.clear();
            data.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int t) {
            int dp3 = dp(3), dp4 = dp(4), dp6 = dp(6), dp8 = dp(8);

            FrameLayout root = new FrameLayout(parent.getContext());
            root.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.setPadding(dp6, dp6, dp6, dp6);
            root.setFocusable(true);
            root.setClickable(true);
            root.setBackground(itemBg(dp8, dp3));

            ImageView img = new ImageView(parent.getContext());
            img.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(0xFF1A2030);
            img.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            root.addView(img);

            TextView name = new TextView(parent.getContext());
            FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.gravity = Gravity.BOTTOM;
            name.setLayoutParams(nlp);
            name.setPadding(dp4, dp4, dp4, dp4);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setTextColor(0xFFCCD6E0);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            name.setGravity(Gravity.CENTER_HORIZONTAL);
            name.setBackgroundColor(0x88000000);
            root.addView(name);

            return new VH(root, img, name);
        }

        /** Fundo do item: o focado ganha uma borda branca. */
        private android.graphics.drawable.Drawable itemBg(int radius, int stroke) {
            GradientDrawable focused = new GradientDrawable();
            focused.setColor(0xFF1A2030);
            focused.setCornerRadius(radius);
            focused.setStroke(stroke, Color.WHITE);
            GradientDrawable normal = new GradientDrawable();
            normal.setColor(0xFF0D1520);
            normal.setCornerRadius(radius);
            normal.setStroke(1, 0x22FFFFFF);
            StateListDrawable sl = new StateListDrawable();
            sl.addState(new int[]{android.R.attr.state_focused}, focused);
            sl.addState(new int[]{}, normal);
            return sl;
        }

        @Override
        public void onBindViewHolder(final VH h, int pos) {
            final File f = data.get(pos);
            h.name.setText(f.getName());
            h.img.setImageBitmap(null);
            new Thread(() -> {
                final Bitmap bmp = decode(f, 256);
                runOnUiThread(() -> {
                    if (h.getBindingAdapterPosition() == data.indexOf(f)) h.img.setImageBitmap(bmp);
                });
            }).start();
            h.itemView.setOnClickListener(v -> applyFile(f));
            h.itemView.setOnFocusChangeListener((v, hasFocus) -> v.setAlpha(hasFocus ? 1f : 0.85f));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        final class VH extends RecyclerView.ViewHolder {
            final ImageView img;
            final TextView name;

            VH(View root, ImageView img, TextView name) {
                super(root);
                this.img = img;
                this.name = name;
            }
        }
    }
}
