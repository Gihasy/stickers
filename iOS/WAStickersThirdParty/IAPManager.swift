
class IAPManager: NSObject {
  static let shared = IAPManager()
    private let allTicketIdentifiers: Set<String> = [
    "centilia_raei_cuddle",
    "centilia_raei_dailystickers1",
    "centilia_animated2",
    "centilia_static2_1",
    "centilia_its_you",
    "centilia_you2",
    "centilia_static3_1",
    "everyday_milk_mocha",
    "milk_daily_1",
    "milkmocha_animated",
    "mocha_daily_1"]
  private override init() {
    super.init()
  }
  
  func getProducts() {
    let request = SKProductsRequest(productIdentifiers: allTicketIdentifiers)
    request.delegate = self
    request.start()
  }
  
  func purchase(product: SKProduct) -> Bool {
    if !IAPManager.shared.canMakePayments() {
        return false
    } else {
      let payment = SKPayment(product: product)
      SKPaymentQueue.default().add(payment)
    }
    return true
  }

  func canMakePayments() -> Bool {
    return SKPaymentQueue.canMakePayments()
  }
}
extension IAPManager: SKProductsRequestDelegate, SKRequestDelegate {

  func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
    let badProducts = response.invalidProductIdentifiers
    let goodProducts = response.products
    
    if !goodProducts.isEmpty {
      ProductsDB.shared.items = response.products
      print("bon ", ProductsDB.shared.items)
    }
    
    print("badProducts ", badProducts)
  }
  
  func request(_ request: SKRequest, didFailWithError error: Error) {
    print("didFailWithError ", error)
    DispatchQueue.main.async {
      print("purchase failed")
    }
  }
  
  func requestDidFinish(_ request: SKRequest) {
    DispatchQueue.main.async {
      print("request did finish ")
    }
  }
  
}
final class ProductsDB: ObservableObject, Identifiable {
  static let shared = ProductsDB()
  var items: [SKProduct] = [] {
    willSet {
      DispatchQueue.main.async {
        self.objectWillChange.send()
      }
    }
  }
}
