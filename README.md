# LargeDraweeView

![Maven Central Version](https://img.shields.io/maven-central/v/me.relex/large-drawee-view)

## Preview

| Hero animations                                                                                        | Long image                                                                                        |
| ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------- |
| ![img](https://raw.githubusercontent.com/ongakuer/LargeDraweeView/main/screenshot/hero-animations.gif) | ![img](https://raw.githubusercontent.com/ongakuer/LargeDraweeView/main/screenshot/long-image.gif) |

## English | [中文](README.zh.md)

Several years ago, I created the [PhotoDraweeView](https://github.com/ongakuer/PhotoDraweeView) project, but it was not suitable in the production environment, more like a demo for learning gesture zooming.

In most cases, Apps use Cloud Object Storage Service (OSS) to load web images. Usually, small images (processed by OSS) are displayed in App Feeds, The original image is loaded only after being clicked. This is a reasonable way to use network resources and mobile memory.

[SubscaleView](https://github.com/davemorrissey/subsampling-scale-image-view) is excellent in terms of image zooming and BitmapRegionDecoder. We only need to integrate Fresco's image cache and user interaction experience.

In fact, LargeDraweeView has been used in our app for many years, Its user interaction experience for viewing images is basically similar to that of WeChat and Weibo.

-   For Hero animations, using Activity/Fragment + [TransitionLayout](largeimage/src/main/java/me/relex/largeimage/TransitionLayout.kt) is better than Activity + [ChangeImageTransform](https://developer.android.com/develop/ui/views/animations/transitions/start-activity) in terms of compatibility and intrusiveness, especially when viewing multiple images.

-   Long image can be auto scaled to fit the view size and provide a smooth vertical scrolling experience

-   Supports custom LoadingViewProvider

-   Supports custom ScaleValueHook

-   Additional pull-down gesture for exiting

We initially use some "magic" to handle immersive status bar/navigation bar and back pressed listening. However, with the later introduction of `WindowInsetsControllerCompat` and `OnBackPressedDispatcher`, LargeDraweeView has gradually become more generalized.

Inspired by Fresco 3.4.0's new file cache invocation, I decided to rewrite this project using Kotlin and open-source it.

## Usage

### Dependencies

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

| LargeDraweeView          | Description                                                              |
| :----------------------- | :----------------------------------------------------------------------- |
| show_loading             | Default True                                                             |
| loading_view_provider    | Custom LoadingViewProvider class                                         |
| enable_pull_down_gesture | Enable pull-down gesture, default False                                  |
| exit_duration            | Exit duration including pull-down exit, double-tap reset and image reset |
| long_image_ratio         | Ratio for long image, default 2.5                                        |
| long_image_min_width     | Minimum width for long image, default 400                                |
| long_image_animation     | Initial animation display for long images, default False                 |

### Code

LargeDraweeView loads image using `LargeImageInfo` data object. `LargeImageInfo.Builder` providing a better way to configure parameters.

```kotlin
val info = LargeImageInfo.Builder().apply {
    lowImageUrl(lowUrl)
    imageUrl(heightUrl)
}.build()

largeDraweeView.load(info)
```

| LargeImageInfo       | Description                                                                                    |
| :------------------- | :--------------------------------------------------------------------------------------------- |
| imageUri             | Uri of the large image                                                                         |
| lowImageUri          | Uri of the low-resolution image (optional)                                                     |
| sharedTransitionRect | Shared element rect, used with TransitionLayout (optional)                                     |
| imageSize            | Original image dimensions for calculated shared element, used with TransitionLayout (optional) |

## Principles

LargeDraweeView is a ViewGroup that combines the features of 3 Views.

Top layer: LoadingView

Middle layer: Fresco PreviewDraweeView, for low-resolution preview and animated image support

Bottom layer: SubscaleView

When loading a web image, LoadingView and PreviewDraweeView are displayed first. At the same time, `FrescoImageLoader` is used to query Fresco’s file cache or handle downloading. And then, it provides a file for SubscaleView. Finally, LoadingView and PreviewDraweeView are hidden.

If you wish to adapt it to other image libraries such as Glide or Coil, it may be easy to accomplish.

The Hero animations implemented by TransitionLayout can refer to [LargePhotoActivity](app/src/main/java/me/relex/sample/largeimage/hero/LargePhotoActivity.kt) and [LargeGalleryActivity](app/src/main/java/me/relex/sample/largeimage/hero/LargeGalleryActivity.kt). Basically, it can be used in the production environment.
