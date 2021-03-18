{
	"BasicDataFormatting": {},
	"IntentPlugin": {
		"intent_action": "com.symbol.datacapturereceiver.RECVR",
		"intent_category": "android.intent.category.DEFAULT",
		"intent_delivery": "BROADCAST",
		"intent_output_enabled": true
	},
	"KeystrokePlugin": {
		"keystroke_action_character": "ASCII_NO_VALUE",
		"keystroke_output_enabled": false
	},
	"MainBundle": {
		"CONFIG_MODE": "CREATE_IF_NOT_EXIST",
		"PACKAGE_NAME": "com.symbol.datacapturereceiver"
	},
	"ScannerPlugin": {
		"Decoders": {
			"decoder_aztec": true,
			"decoder_code128": true,
			"decoder_ean13": true,
		},
		"DecodersParams": {
		},
		"MarginLess": {},
		"MultiBarcode": {},
		"ReaderParams": {},
		"ScanParams": {
			"decode_audio_feedback_uri": "content://media/external/audio/media/null"
		},
		"UpcEan": {},
		"scanner_input_enabled": true,
		"scanner_selection": "auto",
		"scanner_selection_by_identifier": "AUTO"
	},
	"mEnableTimeOutMechanism": true,
	"mProfileName": "com.symbol.datacapturereceiver",
	"mTimeOutMS": 30000
}
