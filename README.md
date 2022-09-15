# Android 13 BLE Bonding Issue Test App

This repository contains a sample Android app and an nRF52840 firmware intended to be used to aid reproduction of an issue reported on Google's issue tracker: https://issuetracker.google.com/issues/242755161.

## Sample Firmware for nRF52840

The `nordic-firmware-nrf52840` directory contains the nRF5 SDK v15.3.0 and an example from Nordic Semiconductor that's been slightly modified to support LE Privacy and Just Works pairing.

### Building the firmware

1. Install [J-Link software and documentation pack](https://www.segger.com/downloads/jlink) and  [SEGGER Embedded Studio (SES)](https://www.segger.com/products/development-tools/embedded-studio/). (This example was tested on J-Link v7.80 and SES v6.34.)
2. Launch the project file `nordic-firmware-nrf52840\nRF5_SDK_15.3.0_59ac345\examples\ble_peripheral\ble_app_hrs\pca10056\s140\ses\ble_app_hrs_pca10056_s140.emProject`.
3. The main firmware application logic is inside `nordic-firmware-nrf52840\nRF5_SDK_15.3.0_59ac345\examples\ble_peripheral\ble_app_hrs\main.c`.
4. Connect a Nordic nRF52840 development kit.
5. In SES, select Build -> Build and Debug. (Note: you may need to exclude `nordic-firmware-nrf52840\nRF5_SDK_15.3.0_59ac345\external\segger_rtt\SEGGER_RTT_Syscalls_SES.c` from the build if it errors.)
6. Stop the debugger.
7. The nRF52840 should be advertising as "Android13FTW".

## Sample Android app

The `android-app` directory contains a barebones Android app intended to work with the specific firmware in the `nordic-firmware-nrf52840\nRF5_SDK_15.3.0_59ac345\examples\ble_peripheral\ble_app_hrs` directory.

### Usage

1. Build and install the app onto an Android 13 device using Android Studio.
2. When the app launches, grant it permission to use Bluetooth for scanning and connecting to nearby peripherals.
3. Hit the "Run Test" button.
4. If the firmware is advertising, the app will connect to it, bond with it, and read a value off the Body Sensor Location characteristic. 
5. After that, you'll need to press Button 1 on the nRF52840 which should trigger a disconnect from the firmware (while remaining bonded) and the app will reconnect to the nRF52840 again. If this reconnection and another subsequent characteristic read are successful, the test is deemed to have been successful for this iteration.
6. Repeat by running steps 3 through 5 again. If the issue is reproduced, the reconnection to the bonded peripheral should fail as described in [this comment](https://issuetracker.google.com/issues/242755161#comment70).
