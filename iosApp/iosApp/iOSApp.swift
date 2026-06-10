import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        MainViewControllerKt.initKoinIos()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
