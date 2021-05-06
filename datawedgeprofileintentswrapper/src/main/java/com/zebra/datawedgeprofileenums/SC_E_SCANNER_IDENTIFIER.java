package com.zebra.datawedgeprofileenums;

/*
Available scanners list
Default is Internal Imager
 */
public enum SC_E_SCANNER_IDENTIFIER
{
    AUTO, //Automatic scanner selection
    INTERNAL_IMAGER, //Built-in imager scanner
    INTERNAL_LASER, //Built-in laser scanner
    INTERNAL_CAMERA, //Built-in camera scanner
    SERIAL_SSI, //Pluggable Z-back scanner for ET50/ET55
    BLUETOOTH_SSI, //RS507 Bluetooth scanner
    BLUETOOTH_RS6000, //RS6000 Bluetooth scanner
    BLUETOOTH_DS3678, //DS3678 Bluetooth scanner
    PLUGABLE_SSI, //Serial SSI scanner RS429 (for use with WT6000)
    PLUGABLE_SSI_RS5000, //Serial SSI scanner RS5000 (for use with WT6000)
    USB_SSI_DS3608; //DS3608 pluggable USB scanner

    @Override
    public String toString()
    {
        return this.name();
    }
}
