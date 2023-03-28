import { StyleSheet, View, Text, Button } from 'react-native';
import RNBluetooth from 'react-native-bluetooth';
import { useState, useEffect } from 'react';

export default function App() {

    const [result, setResult] = useState("")

    useEffect(() => {
    
        checkBluetoothStatus();

        return () => {
            console.log('unmount');
            //RNBluetooth.removeEventListener("change",handleConnection)
        }

    }, []);

    const checkBluetoothStatus = async () => {

        //let status = await RNBluetooth.getStatus();
        let dx = RNBluetooth.addEventListener("change",handleConnection)
        //dx.remove();
    }

    const removeListener = () => {
        RNBluetooth.removeEventListener("change",handleConnection)
        
    }

    handleConnection = (resp) => {
        let {connectionState} = resp.type;  
        console.log('type ', connectionState);
    }

    return (
        <View style={styles.container}>
            <Text>Result: {result}</Text>
            <Button title='Remove Listener' onPress={() => removeListener()} />
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
