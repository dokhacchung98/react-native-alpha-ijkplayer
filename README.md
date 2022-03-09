# react-native-alpha-ijkplayer

Video player for react native, supported H264, H265

## Installation

```sh
npm install react-native-alpha-ijkplayer
```

## Usage

```js
import { AlphaIjkplayerView } from "react-native-alpha-ijkplayer";

// ...

<AlphaIjkplayerView />
```

### Extra Setting in Android
1. For resolving Error: minSdkVersion 16 cannot be smaller than version 21 declared in library [:react-native-alpha-ijkplayer]

   Open up `android/build.gradle`  
    ```
    buildscript {
        ext {
            buildToolsVersion = "28.0.3"
    -        minSdkVersion = 16
    +        minSdkVersion = 21 
            compileSdkVersion = 28
            targetSdkVersion = 28
            supportLibVersion = "28.0.0"

    ```

2. For resolving Error: java.lang.UnsatisfiedLinkError:......couldn't find libffmpeg.so

    Open up `/android/app/build.gradle`
    ```
    android {
         targetSdkVersion rootProject.ext.targetSdkVersion
         versionCode 1
         versionName "1.0"
    +    ndk{
    +       abiFilters "armeabi-v7a", "x86", "x86_64"
    +    }
     }
     splits {
         abi {

    ```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

```
    MIT

    Copyright (c) 2021 khacchung98
```