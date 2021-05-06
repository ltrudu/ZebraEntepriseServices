package com.zebra.printconnectintentswrapper;

import com.zebra.printconnectintentswrapperenums.PC_E_FILEMODE;

import java.util.HashMap;

public class PCTemplateFileNamePrintSettings extends PCIntentsBaseSettings {
    /*
    Store here a template name
     */
    public String mTemplateFileName = "";

    /*
    Store here the data that have to be replaced in the template
    Keep it to null if there are no variable in your template
     */
    public HashMap<String,String> mVariableData = null;

    /*
    Define where the file is located:
        - on the PrintConnect configurated folder
        - on the device
     */
    public PC_E_FILEMODE mFileMode = PC_E_FILEMODE.FILE_SYSTEM;
}

