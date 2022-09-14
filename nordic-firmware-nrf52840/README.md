# Sample Firmware for nRF52840

This directory contains the nRF5 SDK v15.3.0 and an example from Nordic Semiconductor that's been slightly modified to support LE Privacy and Just Works pairing.

## Building the firmware

1. Install [J-Link software and documentation pack](https://www.segger.com/downloads/jlink) and  [SEGGER Embedded Studio (SES)](https://www.segger.com/products/development-tools/embedded-studio/). (This example was tested on J-Link v7.80 and SES v6.34.)
2. Launch the project file `nordic-firmware-nrf52840\nRF5_SDK_15.3.0_59ac345\examples\ble_peripheral\ble_app_hrs\pca10056\s140\ses\ble_app_hrs_pca10056_s140.emProject`.
3. The main firmware application logic is inside `nordic-firmware-nrf52840\nRF5_SDK_15.3.0_59ac345\examples\ble_peripheral\ble_app_hrs\main.c`.
4. Connect a Nordic nRF52840 development kit.
5. In SES, select Build -> Build and Debug. (Note: you may need to exclude `nordic-firmware-nrf52840\nRF5_SDK_15.3.0_59ac345\external\segger_rtt\SEGGER_RTT_Syscalls_SES.c` from the build if it errors.)
6. Stop the debugger.
7. The nRF52840 should be advertising as "Android13FTW".
