English | [简体中文](README.zh.md)

<img src="./image/banner.png" alt="Banner"/>

# RebootNya

[<img src="./image/badges/get-it-on-github.png"
     alt="Get it on Github"
     height="80"
     align="right">](https://github.com/daisukiKaffuChino/RebootNya/releases)

[![GitHub release](https://img.shields.io/github/release/daisukiKaffuChino/RebootNya.svg?logo=github)](https://github.com/daisukiKaffuChino/RebootNya/releases/latest)
[![Crowdin](https://badges.crowdin.net/rebootnya/localized.svg)](https://crowdin.com/project/rebootnya)
[![GitHub license](https://img.shields.io/github/license/daisukiKaffuChino/RebootNya)](https://github.com/daisukiKaffuChino/RebootNya/blob/master/LICENSE)

RebootNya is a simple yet advanced reboot app that supports both **Root** and **[Shizuku](https://shizuku.rikka.app/)**!

Tested on some devices and works well on Android 8.1 to 16.

> On some ROMs’ default launcher, the transparent background may not display correctly (e.g., ColorOS 15). The solution is to switch to another launcher, such as Lawnchair.

## Intent-based Control

RebootNya now supports launching and closing the app via specific intents, allowing integration with external automation tools. Send the following intent to class `github.daisukikaffuchino.rebootnya.MainActivity` to use the feature.

```xml
<!-- Launch app -->
<action android:name="github.daisukikaffuchino.rebootnya.action.LAUNCH" />
<!-- Close app -->
<action android:name="github.daisukikaffuchino.rebootnya.action.CLOSE" />
<!-- Switch interface visibility -->
<action android:name="github.daisukikaffuchino.rebootnya.action.TOGGLE" />
```

## Development Background

One of my old phones has both the power and volume buttons broken, so I urgently needed an advanced reboot app that is aesthetically pleasing, lightweight, and easy to use.

## Contributors

Feel free to dive in! [Open an issue](https://github.com/daisukiKaffuChino/RebootNya/issues/new/choose) or submit pull requests (PRs).

This project exists thanks to all the people who contribute.

![Contributors](https://contrib.rocks/image?repo=daisukiKaffuChino/RebootNya)

## Licenses

- **[RebootNya](https://github.com/daisukiKaffuChino/RebootNya)**: Apache-2.0 license
- **[Android Jetpack](https://github.com/androidx/androidx)**: Apache-2.0 license
- **[Material Components for Android](https://github.com/material-components/material-components-android)**: Apache-2.0 license
- **[libsu](https://github.com/topjohnwu/libsu)**: Apache-2.0 license
- **[RikkaX](https://github.com/RikkaApps/RikkaX)**: MIT license
- **[Shizuku-API](https://github.com/RikkaApps/Shizuku-API)**: Apache-2.0 license

<div align="center">
   <img width="50%" src="https://count.getloli.com/@RebootNya?name=RebootNya&theme=moebooru&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto" alt="counter"></br>
</div>
