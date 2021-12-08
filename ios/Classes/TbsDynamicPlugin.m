#import "TbsDynamicPlugin.h"
#if __has_include(<tbs_dynamic_plugin/tbs_dynamic_plugin-Swift.h>)
#import <tbs_dynamic_plugin/tbs_dynamic_plugin-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "tbs_dynamic_plugin-Swift.h"
#endif

@implementation TbsDynamicPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftTbsDynamicPlugin registerWithRegistrar:registrar];
}
@end
