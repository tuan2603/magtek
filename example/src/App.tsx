import React, { useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  startTransaction,
  refreshList,
  connect,
  disconnect,
  resetDevice,
} from 'react-native-magtek';
import type {
  Transaction,
  Device,
  DeviceConnection,
} from '../../src/interfaces';
import { request, PERMISSIONS, RESULTS } from 'react-native-permissions';
import { Platform, DeviceEventEmitter } from 'react-native';

function App(): React.JSX.Element {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(false);
  const [permissionStatus, setPermissionStatus] = useState<string>('');
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<string>('');

  const handleStartTransaction = async () => {
    try {
      const transaction: Transaction = {
        amount: 1000, // $10.00
        msr: true,
        contact: true,
        contactless: true,
        vas: false,
        gvas: false,
        nfc: false,
        bcr: false,
        quickChip: false,
        emvOnly: false,
        nfcReadOnlyMode: false,
        signature: true,
        fallback: false,
        showAmount: true,
        showTipOptions: false,
        showTax: false,
      };

      console.log(
        'Starting transaction with:',
        JSON.stringify(transaction, null, 2)
      );
      await startTransaction(transaction);
      console.log('Transaction started successfully');
    } catch (error) {
      console.error('Transaction failed:', error);
    }
  };

  const handleRefreshList = async () => {
    try {
      setLoading(true);
      console.log('Refreshing device list...');
      await refreshList();
      // Device list will be received via event
    } catch (error) {
      console.error('Failed to refresh device list:', error);
      setLoading(false);
    }
  };

  // Add event listener for device list
  React.useEffect(() => {
    const subscription = DeviceEventEmitter.addListener(
      'onDeviceListReceived',
      (deviceList: Device[]) => {
        console.log('Device list event received:', deviceList);
        console.log(
          'Device list received:',
          JSON.stringify(deviceList, null, 2)
        );
        setDevices(deviceList);
        setLoading(false);
      }
    );

    const subscriptionConnection = DeviceEventEmitter.addListener(
      'onDeviceConnectionReceived',
      (deviceConnection: DeviceConnection) => {
        console.log('Device connection event received:', deviceConnection);
      }
    );

    return () => {
      subscription.remove();
      subscriptionConnection.remove();
    };
  }, []);

  const handleRequestPermissions = async () => {
    try {
      console.log('Requesting permissions...');

      if (Platform.OS === 'android') {
        const permissions = [
          PERMISSIONS.ANDROID.BLUETOOTH_CONNECT,
          PERMISSIONS.ANDROID.BLUETOOTH_SCAN,
          PERMISSIONS.ANDROID.ACCESS_COARSE_LOCATION,
          PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION,
        ];

        const results = await Promise.all(
          permissions.map((permission) => request(permission))
        );

        const allGranted = results.every(
          (result) => result === RESULTS.GRANTED
        );
        const status = allGranted
          ? 'All permissions granted'
          : 'Some permissions denied';

        console.log('Permission results:', results);
        setPermissionStatus(status);
      } else {
        setPermissionStatus('Permissions not required for iOS');
      }
    } catch (error) {
      console.error('Permission request failed:', error);
      setPermissionStatus('Permission request failed');
    }
  };

  const handleSelectDevice = (device: Device) => {
    setSelectedDevice(device);
    console.log('Selected device:', device);
  };

  const handleConnectDevice = async () => {
    if (!selectedDevice) {
      setConnectionStatus('No device selected');
      return;
    }

    try {
      setConnectionStatus('Connecting...');
      console.log('Connecting to device:', selectedDevice);
      await connect(selectedDevice);
      setConnectionStatus('Connected successfully');
    } catch (error) {
      console.error('Failed to connect to device:', error);
      setConnectionStatus('Connection failed');
    }
  };

  const handleDisconnectDevice = async () => {
    try {
      setConnectionStatus('Disconnecting...');
      await disconnect();
      setConnectionStatus('Disconnected successfully');
      setSelectedDevice(null);
    } catch (error) {
      console.error('Failed to disconnect device:', error);
      setConnectionStatus('Disconnect failed');
    }
  };

  const handleResetDevice = async () => {
    try {
      setConnectionStatus('Resetting device...');
      await resetDevice();
      setConnectionStatus('Device reset successfully');
    } catch (error) {
      console.error('Failed to reset device:', error);
      setConnectionStatus('Reset failed');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#f5f5f5" />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={styles.scrollView}
      >
        <View style={styles.header}>
          <Text style={styles.title}>MagTek React Native Example</Text>
          <Text style={styles.subtitle}>Transaction and Device Management</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Transaction Management</Text>
          <TouchableOpacity
            style={styles.button}
            onPress={handleStartTransaction}
          >
            <Text style={styles.buttonText}>Start Transaction</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Permissions</Text>
          <TouchableOpacity
            style={styles.button}
            onPress={handleRequestPermissions}
          >
            <Text style={styles.buttonText}>Request Permissions</Text>
          </TouchableOpacity>

          {permissionStatus && (
            <Text style={styles.statusText}>Status: {permissionStatus}</Text>
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Device Management</Text>
          <TouchableOpacity
            style={[styles.button, loading && styles.buttonDisabled]}
            onPress={handleRefreshList}
            disabled={loading}
          >
            <Text style={styles.buttonText}>
              {loading ? 'Scanning...' : 'Refresh Device List'}
            </Text>
          </TouchableOpacity>

          {devices.length > 0 && (
            <View style={styles.deviceList}>
              <Text style={styles.deviceListTitle}>Found Devices:</Text>
              {devices.map((device, index) => (
                <TouchableOpacity
                  key={index}
                  style={[
                    styles.deviceItem,
                    selectedDevice?.address === device.address &&
                      styles.selectedDeviceItem,
                  ]}
                  onPress={() => handleSelectDevice(device)}
                >
                  <Text style={styles.deviceName}>{device.name}</Text>
                  <Text style={styles.deviceInfo}>{device.address}</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}

          {selectedDevice && (
            <View style={styles.selectedDeviceSection}>
              <Text style={styles.selectedDeviceTitle}>
                Selected Device: {selectedDevice.name}
              </Text>
              <View style={styles.deviceActions}>
                <TouchableOpacity
                  style={styles.actionButton}
                  onPress={handleConnectDevice}
                >
                  <Text style={styles.buttonText}>Connect</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.actionButton}
                  onPress={handleDisconnectDevice}
                >
                  <Text style={styles.buttonText}>Disconnect</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.actionButton}
                  onPress={handleResetDevice}
                >
                  <Text style={styles.buttonText}>Reset</Text>
                </TouchableOpacity>
              </View>
              {connectionStatus && (
                <Text style={styles.statusText}>
                  Status: {connectionStatus}
                </Text>
              )}
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    padding: 20,
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
  },
  section: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 16,
  },
  button: {
    backgroundColor: '#007AFF',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  deviceList: {
    marginTop: 16,
  },
  deviceListTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  deviceItem: {
    backgroundColor: 'white',
    padding: 16,
    borderRadius: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  deviceName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  deviceInfo: {
    fontSize: 14,
    color: '#666',
    marginBottom: 2,
  },
  statusText: {
    fontSize: 14,
    color: '#007AFF',
    marginTop: 8,
    textAlign: 'center',
  },
  selectedDeviceItem: {
    borderColor: '#007AFF',
    borderWidth: 2,
    backgroundColor: '#f0f8ff',
  },
  selectedDeviceSection: {
    marginTop: 16,
    padding: 16,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  selectedDeviceTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
    textAlign: 'center',
  },
  deviceActions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 12,
  },
  actionButton: {
    backgroundColor: '#28a745',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 6,
    minWidth: 80,
    alignItems: 'center',
  },
});

export default App;
