import { NativeModules, Platform, NativeEventEmitter, NativeAppEventEmitter } from 'react-native';


const LINKING_ERROR =
    `The package 'react-native-bluetooth' doesn't seem to be linked. Make sure: \n\n` +
    Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
    '- You rebuilt the app after installing the package\n' +
    '- You are not using Expo Go\n';

const Bluetooth = NativeModules.Bluetooth
    ? NativeModules.Bluetooth
    : new Proxy(
        {},
        {
            get() {
                throw new Error(LINKING_ERROR);
            },
        }
    );

const BluetoothInfoEventEmitter = new NativeEventEmitter(Bluetooth);

const DEVICE_CONNECTIVITY_EVENT = 'bluetoothDidUpdateState';

const _subscriptions = new Map();

const RNBluetooth = {

    addEventListener: (eventName, handler) => {

        let listener;

        if (eventName === 'change') {
            listener = BluetoothInfoEventEmitter.addListener(
                DEVICE_CONNECTIVITY_EVENT,
                (appStateData) => {
                    handler({
                        type: appStateData,
                    });
                }
            );
        }
        else{
            console.warn('Trying to subscribe to unknown event: "' + eventName + '"');
            return {
                remove: () => {}
            };
        }
        
        console.log('1111:',listener);

        _subscriptions.set(handler, listener);
        return {
            remove: () => RNBluetooth.removeEventListener(eventName, handler)
        };
    },
    removeEventListener: (eventName, handler) => {
        console.log('dddd')
        const listener = _subscriptions.get(handler);
        
        if (!listener) {
            return;
        }
        console.log('listener',listener);
        listener.remove();
        _subscriptions.delete(handler);
    },
    getStatus: (options={}) => {

        let params = null;

        if(Object.keys(options).length > 0){
            params = JSON.stringify(options);
        }

        return Bluetooth.bluetoothStatus(params);
    }
}

export default RNBluetooth;


