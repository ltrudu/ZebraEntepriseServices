package com.zebra.printconnectintentswrapper;

import java.util.HashMap;

public class PCTemplateStringPrintSettings extends PCIntentsBaseSettings {
    /*
    Store here a template name
     */
    public String mTemplateName = "";

    /*
    Store here the data that have to be replaced in the template
    Keep it to null if there are no variable in your template
     */
    public HashMap<String,String> mVariableData = null;

    /*
    Store here the content of the ZPL template
     */
    public String mZPLTemplateString = "";
}

