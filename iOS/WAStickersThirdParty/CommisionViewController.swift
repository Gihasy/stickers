//
//  CommisionViewController.swift
//  Centilia Stickers
//
//  Created by Gilang lie on 22/08/23.
//  Copyright © 2023 WhatsApp. All rights reserved.
//

import Foundation
import UIKit

struct PortfolioItem: Codable {
    let id: Int
    let fimgUrl: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case fimgUrl = "fimg_url"
    }
}

class CommisionViewController: UIViewController {


    @IBOutlet weak var collectionView: UICollectionView!
    var portfolioItems : [PortfolioItem] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.collectionViewLayout = UICollectionViewFlowLayout()
        // Replace with your actual URL
        let jsonURL = URL(string: "https://www.centilia.id/wp-json/wp/v2/portfolios")!

        URLSession.shared.dataTask(with: jsonURL) { data, _, error in
            guard let data = data, error == nil else {
                print("Error fetching data: \(error?.localizedDescription ?? "")")
                return
            }
            
            do {
                let decoder = JSONDecoder()
                self.portfolioItems = try decoder.decode([PortfolioItem].self, from: data)
                DispatchQueue.main.async {
                    self.collectionView.reloadData()
                }

                // You can now use the image URLs for further processing
            } catch {
                print("Error decoding JSON: \(error)")
            }
        }.resume()
    }
}
extension CommisionViewController: UICollectionViewDataSource{
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return portfolioItems.count
    }
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "CommisionCollectionViewCell", for: indexPath) as? CommisionCollectionViewCell else {
            fatalError("Could not dequeue reusable cell")
        }

        let imageUrlString = portfolioItems[indexPath.item].fimgUrl
        
        if let imageUrl = URL(string: imageUrlString) {
            URLSession.shared.dataTask(with: imageUrl) { data, _, error in
                if let data = data {
                    DispatchQueue.main.async {
                        cell.commisionImageView.image = UIImage(data: data)
                    }
                }
            }.resume()
        }
        
        return cell
    }
}

extension CommisionViewController: UICollectionViewDelegateFlowLayout {
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let screenWidth = UIScreen.main.bounds.width
        let itemWidth = (screenWidth - 20) / 2 // Assuming you want 2 images per row with a 10-point spacing
        return CGSize(width: itemWidth, height: itemWidth)
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumLineSpacingForSectionAt section: Int) -> CGFloat {
        return 10 // Adjust this value for the spacing between rows
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, minimumInteritemSpacingForSectionAt section: Int) -> CGFloat {
        return 10 // Adjust this value for the spacing between items in the same row
    }
    
//    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, insetForSectionAt section: Int) -> UIEdgeInsets {
//        return UIEdgeInsets(top: 10, left: 10, bottom: 10, right: 10) // Adjust these values for section insets
//    }
}

