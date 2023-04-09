import CoreBluetooth;

@objc(Bluetooth)
class Bluetooth: RCTEventEmitter, CBCentralManagerDelegate {
    
    private var hasListeners = false;
    //private var DATA: [String] = []
    var DATA = [String]()
    var responsePromise:RCTPromiseResolveBlock!
    var manager:CBCentralManager!
    
    override init() {
        super.init();
        
        manager = CBCentralManager()
        manager.delegate = self
    }
    

    /*@objc(multiply:withB:withResolver:withRejecter:)
    func multiply(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
        resolve(a*b)
    }*/
    
    func centralManagerDidUpdateState(_ central: CBCentralManager){
        
        var getCurrentState: String? = nil

        switch central.state{
            case .unauthorized : getCurrentState = "UNAUTHORIZED";
                    
            case .unsupported : getCurrentState = "UNSUPPORTED";
            
            case .unknown : getCurrentState = "UNKNOWN";
            
            case .resetting : getCurrentState = "RESETTING";
            
            case .poweredOn : getCurrentState = "ENABLE";
            
            case .poweredOff : getCurrentState = "DISABLE";
            
            default: getCurrentState = "UNSUPPORTED";
        }
        
        self.sendEvent(withName: "bluetoothDidUpdateState", body: ["status": getCurrentState])
    }
    
    @objc(bluetoothStatus:withResolver:withRejecter:)
    func bluetoothStatus(params:String?, resolve: @escaping RCTPromiseResolveBlock,
                         reject: RCTPromiseRejectBlock) -> Void {
        
        responsePromise = resolve
        
        if(params==nil){
            //self.logPrint(value: "passed empty")
            
            //self.manager.state
            
            if(self.manager == nil){
                reject("Failed:", "Something went wrong #IOSBluetooth1", nil);
            }
            else{
                self.getBluetoothStatus()
            }
        }
        else{
            
            let str = params!
            
            //self.logPrint(value: str)
            
            let data = Data(str.utf8)

            do {
                // make sure this JSON is in the format we expect
                if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                    // try to read out a string array
                
                    self.getBluetoothStatus()
                    
                }
                else{
                    self.getBluetoothStatus()
                }
            } catch let error as NSError {
                reject("Failed:", "\(error.localizedDescription)", nil);
            }
        }
    }
    
    func resolver(message:String,
                  status: String = "FAILED",
                  data: String = "",
                  actCode: String = "") -> Void {
                
            if(responsePromise==nil){
                return
            }
        
        var output : [String:String] = [:]
        
        output.updateValue(status, forKey: "status")
        output.updateValue(message, forKey: "message")
        output.updateValue(data, forKey: "data")
        output.updateValue(actCode, forKey: "actCode")
        responsePromise(output)
    }
    
    //For Log
    func logPrint(value: String?) {
        if (value == nil) {
            return
        }
        
        responsePromise(value)
    }
    
    // we need to override this method and
    // return an array of event names that we can listen to
    override func supportedEvents() -> [String]! {
        return ["bluetoothDidUpdateState"]
    }
    
    // you also need to add the override attribute
    // on these methods
    override func constantsToExport() -> [AnyHashable: Any] {
        return ["initialCount": 0];
    }
    
    override static func requiresMainQueueSetup() -> Bool {
        return false;
    }
    
    override func startObserving(){
        
        hasListeners = true;
        
        //setup the listener
        
        //let count = 1
        //print("count is \(count)")
        //self.sendEvent(withName: "bluetoothDidUpdateState", body: ["count": count])
        
    }
    
    override func stopObserving(){
        
        hasListeners = false;
        
        //stop the listener
    }
    
    func convertDictToJson(_ params: [String:String]) -> String {
        
        let dictionary = params
        let jsonData = try? JSONSerialization.data(withJSONObject: dictionary, options: [])
        let jsonString = String(data: jsonData!, encoding: .utf8)
        //print(jsonString)
        return jsonString!;
    }
    
    func convertJsonToDict(_ params: String) -> NSDictionary {
        
        let jsonString = params
        let jsonData = jsonString.data(using: .utf8)!
        let dictionary = try? JSONSerialization.jsonObject(with: jsonData, options: .mutableLeaves)
        //print(dictionary)
        return dictionary as! NSDictionary;
    }
    
    func getBluetoothStatus() -> Void{
        
        var getCurrentState: String? = nil
        var getMessage: String? = nil
        
        switch self.manager.state {
            
            case CBManagerState.unauthorized : getCurrentState = "UNAUTHORIZED";getMessage = "Unauthorized Bluetooth";
                
            case CBManagerState.unsupported : getCurrentState = "UNSUPPORTED";getMessage = "Unsupported Bluetooth";
            
            case CBManagerState.unknown : getCurrentState = "UNKNOWN";getMessage = "Unknown Bluetooth";
            
            case CBManagerState.resetting : getCurrentState = "RESETTING";getMessage = "Resetting Bluetooth";
            
            case CBManagerState.poweredOn : getCurrentState = "ENABLE";getMessage = "Bluetooth is ON";
            
            case CBManagerState.poweredOff : getCurrentState = "DISABLE";getMessage = "Bluetooth is OFF";
            
            default: getCurrentState = "UNSUPPORTED";getMessage = "Unknown Bluetooth";
            
        }
        
        //self.logPrint(value: getMessage)
        let output: [String:String] = [
            "status" :getCurrentState!
        ];
        
        let getData = self.convertDictToJson(output)
        
        self.resolver(message:  getMessage!, status: "SUCCESS", data: getData)
    }
    
    
}
