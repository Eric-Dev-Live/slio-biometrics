#import "SlioBiometricsPlugin.h"
#if __has_include(<slio_biometrics/slio_biometrics-Swift.h>)
#import <slio_biometrics/slio_biometrics-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "slio_biometrics-Swift.h"
#endif

@implementation SlioBiometricsPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftSlioBiometricsPlugin registerWithRegistrar:registrar];
}
@end
