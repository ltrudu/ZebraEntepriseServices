package com.zebra.printconnectintentswrapperenums;

public enum PC_E_FILEMODE
{
    // Whe the file mode is set to PRINTCONNECTCONFIGFOLDER the input data will
    // be read from the source folder that has been set using the PrintConnect Settings GUI
    //
    // By default it will read the samples provided in PrintConnect assets folder only
    // You MUST configure it to Local folder if you want to print files stored in the SDCard
    // If you configure it to use Cloud Storage, the file will be loaded from the Cloud
    PRINTCONNECTCONFIGFOLDER("PRINTCONNECTCONFIGFOLDER"),

    // If the file mode is set to FILE_SYSTEM, the file data will
    // be read from the provided path using Android Java file reading API
    // The content will be put in a String object then passed
    // to the TemplatePrintWithContentService Intent API.
    FILE_SYSTEM("FILE_SYSTEM");

    private String enumString;

    private PC_E_FILEMODE(String confName)
    {
        this.enumString = confName;
    }

    public String toString()
    {
        return this.enumString;
    }

    public static PC_E_FILEMODE getFileMode(String type)
    {
        switch (type)
        {
            case "PRINTCONNECTCONFIGFOLDER":
                return PRINTCONNECTCONFIGFOLDER;
            case "FILE_SYSTEM":
                return FILE_SYSTEM;
        }
        return FILE_SYSTEM;
    }
}
