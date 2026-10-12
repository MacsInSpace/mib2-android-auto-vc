> [!WARNING]
> Unfortunately I am not able to maintain this project anymore since I dont own the car anymore.
> If someone is willing to take over the project feel free to contact me

# MIB2 High Android Auto Virtual Cockpit

This project aims to finish what VW started. To bring Navigation and music information from AndroidAuto to the VW Virtual Cockpit.

> [!NOTE]
> This project does not support Car Play. Contributions for Car Play support are very much welcome.

## Features
- [X] Navigation directions
- [X] Distance to next maneuver
- [ ] ~~ETA and arrival time~~ (Not possible since it is not exposed from AndroidAuto)
- [X] Current title information
  - [X] Title
  - [X] Album
  - [X] Artist
  - [X] Cover art
  - [ ] Show which app is currently playing music
- [ ] Prevent forcing navigation view when navigation starts (I don´t yet know if it is possible)
- [ ] Imperial units (contributions welcome)


> This project does not aim to render google maps from android auto in the VC. Refer to good folks over in [this issue](https://github.com/jilleb/mib2-toolbox/issues/159)

### Tested Train Versions

| Train version         | Compatible |
|-----------------------|------------|
| `MHI2_ER_VWG13_P4521` | Y          |
| `MHI2_ER_VWG13_K4525` | Y          |
| `MHI2_ER_VWG13_K4525` | Y          |
| `MHI2_ER_AU43x`       | N          |
| `MHI2_ER_AUG24`       | N          |
| `MHI2_ER_AU62x`       | N          |

Other versions may be compatible. Feel free to give it a try and give feedback.

## Installation

1. Download the latest version of [VCAndroidAuto.jar](https://github.com/adi961/mib2-android-auto-vc/releases/latest/download/VCAndroidAuto.jar)
2. Download the latest version of [installVCAndroidAuto.sh](https://github.com/adi961/mib2-android-auto-vc/releases/latest/download/installVCAndroidAuto.sh)
3. Copy both to the root of your sd card
4. ssh into your mib headunit [guide](https://github.com/jilleb/mib2-toolbox/wiki/SSH-Login)
5. and run `/fs/sda0/installVCAndroidAuto.sh`

## Examples

### Navigation

![Navigation](assets/Navigation.jpg)

### Current title

![Current title](assets/CurrentTitle.jpg)

## Development

Required software: `make`, `java`, `docker`, `unzip`

1. Extract your lsd.jxe from your headunit
2. copy it to the root of this repository
3. run `make` to extract your lsd.jxe and setup the repo
4. to add a class to the patch run `make extractOriginal the.class.path.ClassName`
5. edit the class in the src folder

Based on the great work of [grajen3](https://github.com/grajen3/mib2-lsd-patching) and [andrewleech](https://github.com/andrewleech)
