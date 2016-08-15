package com.chinabike.plugins.mip.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chinabike.plugins.FakeR;
import com.chinabike.plugins.mip.AppManager;
import com.chinabike.plugins.mip.common.ExtraKey;
import com.chinabike.plugins.mip.common.LocalImageHelper;
import com.chinabike.plugins.mip.widget.AlbumViewPager;
import com.chinabike.plugins.mip.widget.MatrixImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Jimmy Wu
 * @Description:相片列表
 * @date 2016年2月22日16:20:11
 */
public class LocalAlbumDetail extends BaseActivity implements MatrixImageView.OnSingleTapListener, CompoundButton.OnCheckedChangeListener {

    GridView gridView;
    TextView title;//标题
    View titleBar;//标题栏
    View pagerContainer;//图片显示部分
    TextView finish, headerFinish;
    AlbumViewPager viewpager;//大图显示pager
    String folder;
    TextView mCountView;
    List<LocalImageHelper.LocalFile> currentFolder = null;

    ImageView mBackView;
    View headerBar;
    CheckBox checkBox;
    List<LocalImageHelper.LocalFile> checkedItems;
    protected Map<String, Integer> fileNames;

    protected ArrayList<String> arrayListForResult;
    protected ArrayList<Integer> rotateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(FakeR.getId(this, "layout", "local_album_detail"));
        Intent intent = getIntent();
        maximumImagesCount = intent.getIntExtra("MAX_IMAGES", 20);
        desiredWidth = intent.getIntExtra("WIDTH", 0);
        desiredHeight = intent.getIntExtra("HEIGHT", 0);
        quality = intent.getIntExtra("QUALITY", 100);
        if (!helper.isInited()) {
            finish();
            return;
        }
        title = (TextView) findViewById(FakeR.getId(this, "id", "album_title"));
        finish = (TextView) findViewById(FakeR.getId(this, "id", "album_finish"));
        headerFinish = (TextView) findViewById(FakeR.getId(this, "id", "header_finish"));
        gridView = (GridView) findViewById(FakeR.getId(this, "id", "gridview"));
        titleBar = findViewById(FakeR.getId(this, "id", "album_title_bar"));
        viewpager = (AlbumViewPager) findViewById(FakeR.getId(this, "id", "albumviewpager"));
        pagerContainer = findViewById(FakeR.getId(this, "id", "pagerview"));
        mCountView = (TextView) findViewById(FakeR.getId(this, "id", "header_bar_photo_count"));
        viewpager.setOnPageChangeListener(pageChangeListener);
        viewpager.setOnSingleTapListener(this);
        mBackView = (ImageView) findViewById(FakeR.getId(this, "id", "header_bar_photo_back"));
        headerBar = findViewById(FakeR.getId(this, "id", "album_item_header_bar"));
        checkBox = (CheckBox) findViewById(FakeR.getId(this, "id", "checkbox"));
        checkBox.setOnCheckedChangeListener(this);
        mBackView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideViewPager();
            }
        });
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.setResultOk(true);
                doActivityResult();
//                new ResizeImagesTask().execute(fileNames.entrySet());
            }
        });
        headerFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.setResultOk(true);
                doActivityResult();
//                new ResizeImagesTask().execute(fileNames.entrySet());
            }
        });
        findViewById(FakeR.getId(this, "id", "album_back")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        folder = getIntent().getExtras().getString(ExtraKey.LOCAL_FOLDER_NAME);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //防止停留在本界面时切换到桌面，导致应用被回收，图片数组被清空，在此处做一个初始化处理
                helper.initImage();
                //获取该文件夹下地所有文件
                final List<LocalImageHelper.LocalFile> folders = helper.getFolder(folder);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (folders != null) {
                            currentFolder = folders;
                            MyAdapter adapter = new MyAdapter(LocalAlbumDetail.this, folders);
                            title.setText(folder);
                            gridView.setAdapter(adapter);
                            //设置当前选中数量
                            if (checkedItems.size() + helper.getCurrentSize() > 0) {
                                finish.setText(String.format("完成(%d/%d)", (checkedItems.size() + helper.getCurrentSize()), maximumImagesCount));
                                finish.setEnabled(true);
                                headerFinish.setText(String.format("完成(%d/%d)", (checkedItems.size() + helper.getCurrentSize()), maximumImagesCount));
                                headerFinish.setEnabled(true);
                            } else {
                                finish.setText("完成");
//                                finish.setEnabled(false);
                                headerFinish.setText("完成");
//                                headerFinish.setEnabled(false);
                            }
                        }
                    }
                });
            }
        }).start();
        checkedItems = helper.getCheckedItems();
        fileNames = helper.getFileNames();
        helper.setResultOk(false);
    }

    private void showViewPager(int index) {
        pagerContainer.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.GONE);
        findViewById(FakeR.getId(this, "id", "album_title_bar")).setVisibility(View.GONE);
        viewpager.setAdapter(viewpager.new LocalViewPagerAdapter(currentFolder));
        viewpager.setCurrentItem(index);
        mCountView.setText((index + 1) + "/" + currentFolder.size());
        //第一次载入第一张图时，需要手动修改
        if (index == 0) {
            checkBox.setTag(currentFolder.get(index));
            checkBox.setChecked(checkedItems.contains(currentFolder.get(index)));
        }
        AnimationSet set = new AnimationSet(true);
        ScaleAnimation scaleAnimation = new ScaleAnimation((float) 0.9, 1, (float) 0.9, 1, pagerContainer.getWidth() / 2, pagerContainer.getHeight() / 2);
        scaleAnimation.setDuration(300);
        set.addAnimation(scaleAnimation);
        AlphaAnimation alphaAnimation = new AlphaAnimation((float) 0.1, 1);
        alphaAnimation.setDuration(200);
        set.addAnimation(alphaAnimation);
        pagerContainer.startAnimation(set);
    }

    private void hideViewPager() {
        pagerContainer.setVisibility(View.GONE);
        gridView.setVisibility(View.VISIBLE);
        findViewById(FakeR.getId(this, "id", "album_title_bar")).setVisibility(View.VISIBLE);
        AnimationSet set = new AnimationSet(true);
        ScaleAnimation scaleAnimation = new ScaleAnimation(1, (float) 0.9, 1, (float) 0.9, pagerContainer.getWidth() / 2, pagerContainer.getHeight() / 2);
        scaleAnimation.setDuration(200);
        set.addAnimation(scaleAnimation);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(200);
        set.addAnimation(alphaAnimation);
        pagerContainer.startAnimation(set);
        ((BaseAdapter) gridView.getAdapter()).notifyDataSetChanged();
    }

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            if (viewpager.getAdapter() != null) {
                String text = (position + 1) + "/" + viewpager.getAdapter().getCount();
                mCountView.setText(text);
                checkBox.setTag(currentFolder.get(position));
                checkBox.setChecked(checkedItems.contains(currentFolder.get(position)));
            } else {
                mCountView.setText("0/0");
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
            // TODO Auto-generated method stub

        }
    };

    @Override
    public void onSingleTap() {
        if (headerBar.getVisibility() == View.VISIBLE) {
            AlphaAnimation animation = new AlphaAnimation(1, 0);
            animation.setDuration(300);
            headerBar.startAnimation(animation);
            headerBar.setVisibility(View.GONE);
        } else {
            headerBar.setVisibility(View.VISIBLE);
            AlphaAnimation animation = new AlphaAnimation(0, 1);
            animation.setDuration(300);
            headerBar.startAnimation(animation);
        }
    }

    private void doActivityResult() {
//        Intent data = new Intent();
        Uri uri;
        Object[] objects;
        String name;
//        Bundle res;

        arrayListForResult = new ArrayList<String>();
        rotateList = new ArrayList<Integer>();
        try {
            if (checkedItems.size() == 0) {
                setResult(Activity.RESULT_CANCELED);
                AppManager.getAppManager().finishActivity(LocalAlbum.class);
                finish();
            } else {
                int index = 0;
                for (LocalImageHelper.LocalFile localFile : checkedItems) {
                    uri = Uri.parse(localFile.getOriginalUri());
                    objects = getImgInfo(uri);
                    name = objects[0].toString();
                    Integer rotate = new Integer(objects[1].toString());
                    arrayListForResult.add(index++, name);
                    rotateList.add(rotate);
                }
//                res = new Bundle();
//                res.putStringArrayList("MULTIPLEFILENAMES", arrayList);
//                data.putExtras(res);
//                setResult(RESULT_OK, data);


//                app.setResultCode(Activity.RESULT_OK);
//                app.setFileNames(arrayListForResult);
                new ResizeImagesTask().execute(fileNames.entrySet());
            }

        } catch (Exception ex) {
//            res = new Bundle();
//            res.putString("ERRORMESSAGE", ex.getMessage());
//            data.putExtras(res);
//            setResult(RESULT_CANCELED, data);


            app.setResultCode(Activity.RESULT_CANCELED);
            app.setErrMsg(ex.getMessage());
            AppManager.getAppManager().finishActivity(LocalAlbum.class);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (pagerContainer.getVisibility() == View.VISIBLE) {
            hideViewPager();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        Object tag = compoundButton.getTag();
        LocalImageHelper.LocalFile localFile = (LocalImageHelper.LocalFile) tag;
        Uri uri = Uri.parse(localFile.getOriginalUri());
        Object[] objects = getImgInfo(uri);
        String name = objects[0].toString();
        Integer rotation = new Integer(objects[1].toString());
        if (!b) {
            if (checkedItems.contains(compoundButton.getTag())) {
                checkedItems.remove(tag);
                fileNames.remove(objects[0].toString());
            }
        } else {
            if (!checkedItems.contains(compoundButton.getTag())) {
                if (checkedItems.size() + helper.getCurrentSize() >= maximumImagesCount) {
                    Toast.makeText(this, String.format("最多选择%d张图片", 9), Toast.LENGTH_SHORT).show();
                    compoundButton.setChecked(false);
                    return;
                }
                checkedItems.add(localFile);
                fileNames.put(name, rotation);
            }
        }
        if (checkedItems.size() + helper.getCurrentSize() > 0) {
            finish.setText(String.format("完成(%d/%d)", (checkedItems.size() + helper.getCurrentSize()), maximumImagesCount));
            finish.setEnabled(true);
            headerFinish.setText(String.format("完成(%d/%d)", (checkedItems.size() + helper.getCurrentSize()), maximumImagesCount));
            headerFinish.setEnabled(true);
        } else {
            finish.setText("完成");
            finish.setEnabled(false);
            headerFinish.setText("完成");
            headerFinish.setEnabled(false);
        }
    }

    public class MyAdapter extends BaseAdapter {
        private Context m_context;
        private LayoutInflater miInflater;
        DisplayImageOptions options;
        List<LocalImageHelper.LocalFile> paths;

        public MyAdapter(Context context, List<LocalImageHelper.LocalFile> paths) {
            m_context = context;
            this.paths = paths;
            options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(false)
                    .showImageForEmptyUri(FakeR.getId(context, "drawable", "cb_no_pic_small"))
                    .showImageOnFail(FakeR.getId(context, "drawable", "cb_no_pic_small"))
                    .showImageOnLoading(FakeR.getId(context, "drawable", "cb_no_pic_small"))
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .setImageSize(new ImageSize(app.getQuarterWidth(), 0))
                    .displayer(new SimpleBitmapDisplayer()).build();
        }

        @Override
        public int getCount() {
            return paths.size();
        }

        @Override
        public LocalImageHelper.LocalFile getItem(int i) {
            return paths.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int i, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder = new ViewHolder();

            if (convertView == null || convertView.getTag() == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(FakeR.getId(m_context, "layout", "simple_list_item"), null);
                viewHolder.imageView = (ImageView) convertView.findViewById(FakeR.getId(m_context, "id", "imageView"));
                viewHolder.checkBox = (CheckBox) convertView.findViewById(FakeR.getId(m_context, "id", "checkbox"));
                viewHolder.checkBox.setOnCheckedChangeListener(LocalAlbumDetail.this);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            ImageView imageView = viewHolder.imageView;
            LocalImageHelper.LocalFile localFile = paths.get(i);
//            FrescoLoader.getInstance().localDisplay(localFile.getThumbnailUri(), imageView, options);
            ImageLoader.getInstance().displayImage(localFile.getThumbnailUri(), new ImageViewAware(viewHolder.imageView), options,
                    loadingListener, null, localFile.getOrientation());
            viewHolder.checkBox.setTag(localFile);
            viewHolder.checkBox.setChecked(checkedItems.contains(localFile));
            viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showViewPager(i);
                }
            });
            return convertView;
        }

        private class ViewHolder {
            ImageView imageView;
            CheckBox checkBox;
        }
    }

    private class ResizeImagesTask extends AsyncTask<Set<Map.Entry<String, Integer>>, Void, ArrayList<String>> {
        private Exception asyncTaskError = null;

        @Override
        protected ArrayList<String> doInBackground(Set<Map.Entry<String, Integer>>... fileSets) {
            Set<Map.Entry<String, Integer>> fileNames = fileSets[0];
            ArrayList<String> al = new ArrayList<String>();
            try {
//                Iterator<Map.Entry<String, Integer>> i = fileNames.iterator();
                Bitmap bmp;
//                while (i.hasNext()) {
                int index = 0;
                for (String realPath : arrayListForResult) {
//                    Map.Entry<String, Integer> imageInfo = i.next();
//                    File file = new File(imageInfo.getKey());
                    File file = new File(realPath);
//                    imageInfo.
//                    int rotate = imageInfo.getValue().intValue();
                    int rotate = rotateList.get(index++).intValue();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                    int width = options.outWidth;
                    int height = options.outHeight;
                    float scale = calculateScale(width, height);
                    if (scale < 1) {
                        int finalWidth = (int) (width * scale);
                        int finalHeight = (int) (height * scale);
                        int inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight);
                        options = new BitmapFactory.Options();
                        options.inSampleSize = inSampleSize;
                        try {
                            bmp = this.tryToGetBitmap(file, options, rotate, true);
                        } catch (OutOfMemoryError e) {
                            options.inSampleSize = calculateNextSampleSize(options.inSampleSize);
                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                throw new IOException("Unable to load image into memory.");
                            }
                        }
                    } else {
                        try {
                            bmp = this.tryToGetBitmap(file, null, rotate, false);
                        } catch (OutOfMemoryError e) {
                            options = new BitmapFactory.Options();
                            options.inSampleSize = 2;
                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                options = new BitmapFactory.Options();
                                options.inSampleSize = 4;
                                try {
                                    bmp = this.tryToGetBitmap(file, options, rotate, false);
                                } catch (OutOfMemoryError e3) {
                                    throw new IOException("Unable to load image into memory.");
                                }
                            }
                        }
                    }

                    file = this.storeImage(bmp, file.getName());
                    al.add(Uri.fromFile(file).toString());
                }
                return al;
            } catch (IOException e) {
                try {
                    asyncTaskError = e;
                    for (int i = 0; i < al.size(); i++) {
                        URI uri = new URI(al.get(i));
                        File file = new File(uri);
                        file.delete();
                    }
                } catch (Exception exception) {
                    // the finally does what we want to do
                } finally {
                    return new ArrayList<String>();
                }
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> al) {
//            Intent data = new Intent();

            if (asyncTaskError != null) {
//                Bundle res = new Bundle();
//                res.putString("ERRORMESSAGE", asyncTaskError.getMessage());
//                data.putExtras(res);
//                setResult(RESULT_CANCELED, data);

                app.setResultCode(RESULT_CANCELED);
                app.setErrMsg(asyncTaskError.getMessage());

            } else if (al.size() > 0) {
//                Bundle res = new Bundle();
//                res.putStringArrayList("MULTIPLEFILENAMES", al);
//                data.putExtras(res);
//                setResult(RESULT_OK, data);

                app.setResultCode(RESULT_OK);
                app.setFileNames(al);

            } else {
//                setResult(RESULT_CANCELED, data);
                app.setResultCode(RESULT_CANCELED);
                app.setErrMsg(asyncTaskError.getMessage());
            }

            AppManager.getAppManager().finishActivity(LocalAlbum.class);
            finish();
        }

        private Bitmap tryToGetBitmap(File file, BitmapFactory.Options options, int rotate, boolean shouldScale) throws IOException, OutOfMemoryError {
            Bitmap bmp;
            if (options == null) {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            }
            if (bmp == null) {
                throw new IOException("The image file could not be opened.");
            }
            if (options != null && shouldScale) {
                float scale = calculateScale(options.outWidth, options.outHeight);
                bmp = this.getResizedBitmap(bmp, scale);
            }
            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }
            return bmp;
        }

        /*
        * The following functions are originally from
        * https://github.com/raananw/PhoneGap-Image-Resizer
        *
        * They have been modified by Andrew Stephan for Sync OnSet
        *
        * The software is open source, MIT Licensed.
        * Copyright (C) 2012, webXells GmbH All Rights Reserved.
        */
        private File storeImage(Bitmap bmp, String fileName) throws IOException {
            int index = fileName.lastIndexOf('.');
            String name = fileName.substring(0, index);
            String ext = fileName.substring(index);
            File file = File.createTempFile("tmp_" + name, ext);
            OutputStream outStream = new FileOutputStream(file);
            if (ext.compareToIgnoreCase(".png") == 0) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality, outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream);
            }
            outStream.flush();
            outStream.close();
            return file;
        }

        private Bitmap getResizedBitmap(Bitmap bm, float factor) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(factor, factor);
            // recreate the new Bitmap
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            return resizedBitmap;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private int calculateNextSampleSize(int sampleSize) {
        double logBaseTwo = (int) (Math.log(sampleSize) / Math.log(2));
        return (int) Math.pow(logBaseTwo + 1, 2);
    }

    private float calculateScale(int width, int height) {
        float widthScale = 1.0f;
        float heightScale = 1.0f;
        float scale = 1.0f;
        if (desiredWidth > 0 || desiredHeight > 0) {
            if (desiredHeight == 0 && desiredWidth < width) {
                scale = (float) desiredWidth / width;
            } else if (desiredWidth == 0 && desiredHeight < height) {
                scale = (float) desiredHeight / height;
            } else {
                if (desiredWidth > 0 && desiredWidth < width) {
                    widthScale = (float) desiredWidth / width;
                }
                if (desiredHeight > 0 && desiredHeight < height) {
                    heightScale = (float) desiredHeight / height;
                }
                if (widthScale < heightScale) {
                    scale = widthScale;
                } else {
                    scale = heightScale;
                }
            }
        }

        return scale;
    }

    SimpleImageLoadingListener loadingListener = new SimpleImageLoadingListener() {
        @Override
        public void onLoadingComplete(String imageUri, View view, final Bitmap bm) {
            if (TextUtils.isEmpty(imageUri)) {
                return;
            }
            //由于很多图片是白色背景，在此处加一个#eeeeee的滤镜，防止checkbox看不清
            try {
                ((ImageView) view).getDrawable().setColorFilter(Color.argb(0xff, 0xee, 0xee, 0xee), PorterDuff.Mode.MULTIPLY);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}