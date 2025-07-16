// Device-related constants
export const DEVICE_CONSTANTS = {
  // Default timeouts
  DEFAULT_TIMEOUT: 30000, // 30 seconds
  DEFAULT_PIN_TIMEOUT: 60000, // 60 seconds
  DEFAULT_PAN_TIMEOUT: 45000, // 45 seconds

  // Default values
  DEFAULT_MIN_PIN_LENGTH: 4,
  DEFAULT_MAX_PIN_LENGTH: 12,
  DEFAULT_PIN_MODE: 0,
  DEFAULT_PIN_TONE: 1,
  DEFAULT_PIN_FORMAT: 0,

  // Error codes
  ERROR_CODES: {
    SUCCESS: 0,
    TIMEOUT: 1,
    ERROR: 2,
    UNAVAILABLE: 3,
    INVALID_PARAMETER: 4,
    DEVICE_NOT_CONNECTED: 5,
    TRANSACTION_IN_PROGRESS: 6,
  },

  // Connection states
  CONNECTION_STATES: {
    UNKNOWN: 'Unknown',
    DISCONNECTED: 'Disconnected',
    CONNECTING: 'Connecting',
    ERROR: 'Error',
    CONNECTED: 'Connected',
    DISCONNECTING: 'Disconnecting',
  },

  // Device types
  DEVICE_TYPES: {
    SCRA: 'SCRA',
    PPSCRA: 'PPSCRA',
    CMF: 'CMF',
    MMS: 'MMS',
  },

  // Connection types
  CONNECTION_TYPES: {
    USB: 'USB',
    BLUETOOTH_LE: 'BLUETOOTH_LE',
    BLUETOOTH_LE_EMV: 'BLUETOOTH_LE_EMV',
    BLUETOOTH_LE_EMVT: 'BLUETOOTH_LE_EMVT',
    TCP: 'TCP',
    TCP_TLS: 'TCP_TLS',
    TCP_TLS_TRUST: 'TCP_TLS_TRUST',
    WEBSOCKET: 'WEBSOCKET',
    WEBSOCKET_TRUST: 'WEBSOCKET_TRUST',
    MQTT: 'MQTT',
    SERIAL: 'SERIAL',
    AIDL: 'AIDL',
    VIRTUAL: 'VIRTUAL',
  },

  // Payment methods
  PAYMENT_METHODS: {
    MSR: 'MSR',
    CONTACT: 'Contact',
    CONTACTLESS: 'Contactless',
    MANUAL_ENTRY: 'ManualEntry',
    BARCODE: 'Barcode',
    BARCODE_ENCRYPTED: 'BarcodeEncrypted',
    APPLE_VAS: 'AppleVAS',
    NFC: 'NFC',
    GOOGLE_VAS: 'GoogleVAS',
  },

  // Status codes
  STATUS_CODES: {
    SUCCESS: 'SUCCESS',
    TIMEOUT: 'TIMEOUT',
    ERROR: 'ERROR',
    UNAVAILABLE: 'UNAVAILABLE',
  },

  // Image types
  IMAGE_TYPES: {
    BITMAP: 'BITMAP',
  },

  // Barcode types
  BARCODE_TYPES: {
    QRCODE: 'QRCODE',
  },

  // Barcode formats
  BARCODE_FORMATS: {
    BLOB: 'BLOB',
    COMMAND: 'COMMAND',
    BLOB_BASE64: 'BLOB_BASE64',
    COMMAND_BASE64: 'COMMAND_BASE64',
  },

  // VAS modes
  VAS_MODES: {
    SINGLE: 'Single',
    DUAL: 'Dual',
    VAS_ONLY: 'VASOnly',
  },

  // VAS protocols
  VAS_PROTOCOLS: {
    URL: 'URL',
    FULL: 'Full',
  },
} as const;
