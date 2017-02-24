package com.chinabike.plugins.mip.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chinabike.plugins.FakeR;
import com.chinabike.plugins.mip.AppContext;
import com.chinabike.plugins.mip.common.ExtraKey;
import com.chinabike.plugins.mip.common.ImageUtils;
import com.chinabike.plugins.mip.common.LocalImageHelper;
import com.chinabike.plugins.mip.common.StringUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Jimmy Wu on 2016-2-22 16:20:35.
 * 本地相册
 */
public class LocalAlbum extends BaseActivity {
    ListView listView;
    ImageView progress;
    //    View camera;
    List<String> folderNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocalImageHelper.init((AppContext)getApplication());
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        maximumImagesCount = intent.getIntExtra("MAX_IMAGES", 20);
        desiredWidth = intent.getIntExtra("WIDTH", 0);
        desiredHeight = intent.getIntExtra("HEIGHT", 0);
        quality = intent.getIntExtra("QUALITY", 100);
        setContentView(FakeR.getId(this, "layout", "local_album"));
        listView = (ListView) findViewById(FakeR.getId(this, "id", "local_album_list"));
//        camera = findViewById(R.id.loacal_album_camera);
//        camera.setOnClickListener(onClickListener);
//        camera.setVisibility(View.GONE);
        progress = (ImageView) findViewById(FakeR.getId(this, "id", "progress_bar"));
        Animation animation = AnimationUtils.loadAnimation(this, FakeR.getId(this, "anim", "rotate_loading"));
        progress.startAnimation(animation);
        findViewById(FakeR.getId(this, "id", "album_back")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                app.reloadImages();
                //LocalImageHelper.init(app);
                //开启线程初始化本地图片列表，该方法是synchronized的，因此当AppContent在初始化时，此处阻塞
                helper.initImage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //初始化完毕后，显示文件夹列表
                        if (!isDestroy) {
                            initAdapter();
                            progress.clearAnimation();
                            ((View) progress.getParent()).setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
//                            camera.setVisibility(View.VISIBLE);
                            startAllImages(0);
                        }
                    }
                });
            }
        }).start();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startAllImages(i);
            }
        });
    }

    private void startAllImages(int i) {
        Intent intent = new Intent(LocalAlbum.this, LocalAlbumDetail.class);
        intent.putExtra(ExtraKey.LOCAL_FOLDER_NAME, folderNames.get(i));
        intent.putExtra("MAX_IMAGES", maximumImagesCount);
        intent.putExtra("WIDTH", desiredWidth);
        intent.putExtra("HEIGHT", desiredHeight);
        intent.putExtra("QUALITY", quality);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
//        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    public void initAdapter() {
        listView.setAdapter(new FolderAdapter(this, helper.getFolderMap()));
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (helper.getCurrentSize() + helper.getCheckedItems().size() >= maximumImagesCount) {
                Toast.makeText(LocalAlbum.this, String.format("最多选择%d张图片", 9), Toast.LENGTH_SHORT).show();
                return;
            }
            //  拍照后保存图片的绝对路径
            String cameraPath = LocalImageHelper.getInstance().setCameraImgPath();
            File file = new File(cameraPath);
            //添加
            Uri contentUri  = FileProvider.getUriForFile(getApplicationContext(),
                    "com.bike.main.provider", file);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
           // intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //这一步很重要。给目标应用一个临时的授权。
            startActivityForResult(intent,
                    ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA);
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA:
                    String cameraPath = LocalImageHelper.getInstance().getCameraImgPath();
                    if (StringUtils.isEmpty(cameraPath)) {
                        Toast.makeText(this, "图片获取失败", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File file = new File(cameraPath);
                    if (file.exists()) {
                        Uri uri = Uri.fromFile(file);
                        LocalImageHelper.LocalFile localFile = new LocalImageHelper.LocalFile();
                        localFile.setThumbnailUri(uri.toString());
                        localFile.setOriginalUri(uri.toString());
                        localFile.setOrientation(getBitmapDegree(cameraPath));
                        Object[] objects = getImgInfo(uri);
                        String name = objects[0].toString();
                        Integer rotation = new Integer(objects[1].toString());
                        helper.getCheckedItems().add(localFile);
                        helper.getFileNames().put(name, rotation);
                        helper.setResultOk(true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                        //这里本来有个弹出progressDialog的，在拍照结束后关闭，但是要延迟1秒，原因是由于三星手机的相机会强制切换到横屏，
                        //此处必须等它切回竖屏了才能结束，否则会有异常
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 1000);
                    } else {
                        Toast.makeText(this, "图片获取失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 读取图片的旋转的角度，还是三星的问题，需要根据图片的旋转角度正确显示
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    private int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public class FolderAdapter extends BaseAdapter {
        Map<String, List<LocalImageHelper.LocalFile>> folders;
        Context context;
        DisplayImageOptions options;

        FolderAdapter(Context context, Map<String, List<LocalImageHelper.LocalFile>> folders) {
            this.folders = folders;
            this.context = context;
            folderNames = new ArrayList<String>();

            options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(false)
                    .showImageForEmptyUri(FakeR.getId(context, "drawable", "cb_no_pic_small"))
                    .showImageOnFail(FakeR.getId(context, "drawable", "cb_no_pic_small"))
                    .showImageOnLoading(FakeR.getId(context, "drawable", "cb_no_pic_small"))
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .setImageSize(new ImageSize(app.getQuarterWidth(), 0))
                    .displayer(new SimpleBitmapDisplayer()).build();

            Iterator iter = folders.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                folderNames.add(key);
            }
            //根据文件夹内的图片数量降序显示
            Collections.sort(folderNames, new Comparator<String>() {
                public int compare(String arg0, String arg1) {
                    Integer num1 = helper.getFolder(arg0).size();
                    Integer num2 = helper.getFolder(arg1).size();
                    return num2.compareTo(num1);
                }
            });
        }

        @Override
        public int getCount() {
            return folders.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (convertView == null || convertView.getTag() == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(FakeR.getId(context, "layout", "item_albumfoler"), null);
                viewHolder.imageView = (ImageView) convertView.findViewById(FakeR.getId(context, "id", "imageView"));
                viewHolder.textView = (TextView) convertView.findViewById(FakeR.getId(context, "id", "textview"));
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            String name = folderNames.get(i);
            List<LocalImageHelper.LocalFile> files = folders.get(name);
            viewHolder.textView.setText(name + "(" + files.size() + ")");
            if (files.size() > 0) {
                ImageLoader.getInstance().displayImage(files.get(0).getThumbnailUri(), new ImageViewAware(viewHolder.imageView), options,
                        null, null, files.get(0).getOrientation());
            }
            return convertView;
        }

        private class ViewHolder {
            ImageView imageView;
            TextView textView;
        }
    }
}
