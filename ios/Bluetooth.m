#import <React/RCTBridgeModule.h>
#import <React/RCTLog.h>
#import "React/RCTEventEmitter.h"
//#import <CoreBluetooth/CoreBluetooth.h>

@interface RCT_EXTERN_MODULE(Bluetooth, RCTEventEmitter)

RCT_EXTERN_METHOD(multiply:(float)a withB:(float)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

/*RCT_EXTERN_METHOD(bluetoothStatus:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject
                  )*/

RCT_EXTERN_METHOD(bluetoothStatus:(NSString)a
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject
                  )


@end
