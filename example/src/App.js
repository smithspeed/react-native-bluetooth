import { StyleSheet, View, Text, Button } from 'react-native';
import RNBluetooth from 'react-native-bluetooth';
import { useState, useEffect } from 'react';

export default function App() {

    const [result, setResult] = useState("")

    useEffect(() => {
    
        checkBluetoothStatus();

        return () => {
            //console.log('unmount');
            //RNBluetooth.removeEventListener("change",handleConnection)
        }

    }, []);

    const checkBluetoothStatus = async () => {

        let data = await RNBluetooth.getStatus({
            requestToEnable : true
        });

        let obj = JSON.parse(data.data);
        
        //console.log(obj['status']);
        setResult(obj['status']);
        
    }

    const listenOnChangeBluetoothState = () => {
        RNBluetooth.addEventListener("change",handleConnection)
    }

    const removeListener = () => {
        RNBluetooth.removeEventListener("change",handleConnection)
        
    }

    handleConnection = (resp) => {
        //let {connectionState} = resp.type;  
        console.log('type ', resp);
    }

    return (
        <View style={styles.container}>
            <Text>Result: {result}</Text>
            <Text></Text>
            <Button title='Remove Listener' style={{marginBottom:20}} onPress={() => removeListener()} />
            <Text></Text>
            <Button title='Add Listener' onPress={() => listenOnChangeBluetoothState()} />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    box: {
        width: 60,
        height: 60,
        marginVertical: 20,
    },
});
