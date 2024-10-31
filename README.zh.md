# LargeDraweeView

![Maven Central Version](https://img.shields.io/maven-central/v/me.relex/large-drawee-view)

## 前言

多年前我写过一个 [PhotoDraweeView](https://github.com/ongakuer/PhotoDraweeView) 项目，但并没有投入到生成环境中，更像一个学习手势缩放的 Demo。

因为大部分情况下 App 会使用云存储（OSS）来加载远程图片，通常在信息流中使用 OSS 处理过的小图，点击图片后再加载原图查看。这样才是合理的使用网络资源和手机内存。

·[SubscaleView](<(https://github.com/davemorrissey/subsampling-scale-image-view)>) 在图片缩放和 BitmapRegionDecoder 方面足够优秀，只需要处理好 Fresco 图片缓存使用和整合的交互体验即可。

实际上 LargeDraweeView 已经在我们 App 中使用了很多年，基本上与微信，微博等图片查看的交互体验很相似

-   在 Hero 动画方面，使用 Activity/Fragment + [TransitionLayout](largeimage/src/main/java/me/relex/largeimage/TransitionLayout.kt) 比 Activity + [ChangeImageTransform](https://developer.android.com/develop/ui/views/animations/transitions/start-activity) 等低侵入性和兼容性要更好（特别是多图查看情况）

-   长图自适应处理，满足条件时会计算缩放，让图片默认从上到下滚动查看

-   支持自定义的 LoadingViewProvider

-   支持自定义处理缩放 ScaleValueHook

-   额外的下拉手势退出

因为在内部很早就使用，早期需要用“魔法”处理沉浸式状态栏/导航栏，后退返回监听等。 不过随着后来 `WindowInsetsControllerCompat`， `OnBackPressedDispatcher` 出现，这个控件也就逐渐通用了起来。

前段时间 Fresco 发布新版 3.4.0，调整了文件缓存的调用，想着便重新用 kotlin 改写一下项目并开源出来。

## 使用

### 依赖

```groovy
implementation('com.facebook.fresco:fresco:3.4.0')
implementation('com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0')
implementation('me.relex:large-drawee-view:1.0.2')
```

### XML

```xml
<me.relex.largeimage.LargeDraweeView
    android:id="@+id/image_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:show_loading="true"
    app:loading_view_provider=".loading.ImageLoadingViewProvider"
    app:enable_pull_down_gesture="true"
    app:exit_duration="300"
    app:long_image_animation="true" />
```

| LargeDraweeView          | 说明                                       |
| :----------------------- | :----------------------------------------- |
| show_loading             | 默认 True                                  |
| loading_view_provider    | 自定义 LoadingView 提供类                  |
| enable_pull_down_gesture | 开启下拉手势，默认 False                   |
| exit_duration            | 退出时长，包含下拉退出，双击还原，图片重置 |
| long_image_ratio         | 长图判断比例，默认 2.5                     |
| long_image_min_width     | 长图判断最小宽度，默认 400                 |
| long_image_animation     | 长图的初始显示动画，默认 False             |

### Code

LargeDraweeView 使用 LargeImageInfo 数据对象进行加载。使用 LargeImageInfo.Builder 能更好的封装数据

```kotlin
val info = LargeImageInfo.Builder().apply {
    lowImageUrl(lowUrl)
    imageUrl(heightUrl)
}.build()

largeDraweeView.load(info)
```

| LargeImageInfo       | 说明                                                    |
| :------------------- | :------------------------------------------------------ |
| imageUri             | 大图 Uri                                                |
| lowImageUri          | 小图 Uri（可选）                                        |
| sharedTransitionRect | 共享元素位置，配合 TransitionLayout（可选）             |
| imageSize            | 共享元素提前计算原图宽高，配合 TransitionLayout（可选） |

### 原理

LargeDraweeView 内部原理很简单，只是 3 个 View 的包装

上层：LoadingView

中层：Fresco PreviewDraweeView，用于小图占位展示和动图支持

下层：SubscaleView

加载网络图片时，先显示 LoadingView 和 PreviewDraweeView。同时使用 FrescoImageLoader 查询 Fresco 文件缓存和下载等。 最后封装成 File 提供给 SubscaleView 使用，并隐藏 LoadingView 和 PreviewDraweeView

举一反三的话，如果是使用 glide、coil 等其他图片库也很方便就能移植使用。

TransitionLayout 实现的 Hero 过渡效果，我觉得参考 [LargePhotoActivity](app/src/main/java/me/relex/sample/largeimage/hero/LargePhotoActivity.kt) 和 [LargeGalleryActivity](app/src/main/java/me/relex/sample/largeimage/hero/LargeGalleryActivity.kt) 调整就能放到生成环境中使用。

## 预览图

### Hero 动画效果

![img](https://raw.githubusercontent.com/ongakuer/LargeDraweeView/main/screenshot/hero-animations.gif)

### 长图显示

![img](https://raw.githubusercontent.com/ongakuer/LargeDraweeView/main/screenshot/long-image.gif)
