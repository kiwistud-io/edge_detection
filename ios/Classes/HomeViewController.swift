import WeScan
import Flutter
import Foundation
import UIKit

class HomeViewController: UIViewController, CameraScannerViewOutputDelegate, ImageScannerControllerDelegate {
    func captureImageFailWithError(error: Error) {
        print(error)
    }
    
    func captureImageSuccess(image: UIImage, withQuad quad: Quadrilateral?) {
        cameraController?.dismiss(animated: true)
        
        hideButtons()
        let scannerVC = ImageScannerController(image: image, delegate: self)
        if #available(iOS 13.0, *) {
            scannerVC.isModalInPresentation = true
        }
        present(scannerVC, animated: true)
    }
    
    
    var cameraController: CameraScannerViewController!
    var _result:FlutterResult?
    
    override func viewDidAppear(_ animated: Bool) {
        if self.isBeingPresented {
            cameraController = CameraScannerViewController()
            cameraController.delegate = self
            if #available(iOS 13.0, *) {
                cameraController.isModalInPresentation = true
            }
            
            // Temp fix for https://github.com/WeTransfer/WeScan/issues/320
            if #available(iOS 15, *) {
                let appearance = UINavigationBarAppearance()
                let navigationBar = UINavigationBar()
                appearance.configureWithOpaqueBackground()
                appearance.titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.label]
                appearance.backgroundColor = .systemBackground
                navigationBar.standardAppearance = appearance;
                UINavigationBar.appearance().scrollEdgeAppearance = appearance
                
                let appearanceTB = UITabBarAppearance()
                appearanceTB.configureWithOpaqueBackground()
                appearanceTB.backgroundColor = .systemBackground
                UITabBar.appearance().standardAppearance = appearanceTB
                UITabBar.appearance().scrollEdgeAppearance = appearanceTB
            }
            
            present(cameraController, animated: true) {
                
                if let window = UIApplication.shared.keyWindow {
                    window.addSubview(self.topContainerView)
                    window.addSubview(self.containerView)
                    window.addSubview(self.selectPhotoButton)
                    window.addSubview(self.shutterButton)
                    window.addSubview(self.cancelButton)
                    
                    self.setupConstraints()
                }
            }
        }
    }
    
    private lazy var containerView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.white
        view.sizeToFit()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
        }()
    
    private lazy var topContainerView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.white
        view.sizeToFit()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
        }()
    
    
    //촬영버튼
    private lazy var shutterButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "camera", in: Bundle(for: SwiftEdgeDetectionPlugin.self), compatibleWith: nil)?.withRenderingMode(.alwaysOriginal), for: .normal)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(captureImage(_:)), for: .touchUpInside)
        return button
    }()
    
    //취소버튼
    private lazy var cancelButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "back", in: Bundle(for: SwiftEdgeDetectionPlugin.self), compatibleWith: nil)?.withRenderingMode(.alwaysOriginal), for: .normal)
        button.setTitle(NSLocalizedString("cancel", tableName: nil, bundle: Bundle(for: ScannerViewController.self), value: " Back", comment: "The cancel button"), for: .normal)
        button.setTitleColor(.gray, for: .normal)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(cancelImageScannerController), for: .touchUpInside)
        return button
    }()
    
    //앨범버튼
    lazy var selectPhotoButton: UIButton = {
        let button = UIButton()
        //버튼 이미지 세팅
        
        button.setImage(UIImage(named: "album", in: Bundle(for: SwiftEdgeDetectionPlugin.self), compatibleWith: nil)?.withRenderingMode(.alwaysOriginal), for: .normal)
        button.contentMode = .scaleToFill
        button.frame.size.width = 50
        button.frame.size.height = 50
        
        button.addTarget(self, action: #selector(selectPhoto), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    // MARK: - Actions
    
    @objc private func cancelImageScannerController() {
        hideButtons()
        _result!(nil)
        
        cameraController?.dismiss(animated: true)
        dismiss(animated: true)
    }
    
    @objc private func captureImage(_ sender: UIButton) {
        shutterButton.isUserInteractionEnabled = false
        cameraController?.capture()
    }
    
    @objc func selectPhoto() {
        if let window = UIApplication.shared.keyWindow {
            window.rootViewController?.dismiss(animated: true, completion: nil)
            self.hideButtons()
            
            let scanPhotoVC = ScanPhotoViewController()
            scanPhotoVC._result = _result
            if #available(iOS 13.0, *) {
                scanPhotoVC.isModalInPresentation = true
            }
            window.rootViewController?.present(scanPhotoVC, animated: true)
        }
    }
    
    func hideButtons() {
        cancelButton.isHidden = true
        selectPhotoButton.isHidden = true
        shutterButton.isHidden = true
        containerView.isHidden = true
        topContainerView.isHidden=true
    }
    

    
    private func setupConstraints() {
        var cancelButtonConstraints = [NSLayoutConstraint]()
        var selectPhotoButtonConstraints = [NSLayoutConstraint]()
        var shutterButtonConstraints = [
            NSLayoutConstraint
        ]()
        let containerViewConstraints = [
            containerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            containerView.widthAnchor.constraint(equalToConstant: 500.0),
            containerView.heightAnchor.constraint(equalToConstant: 117.0)
        ]
        let topContainerViewConstraints = [
            topContainerView.widthAnchor.constraint(equalToConstant: 500.0),
            topContainerView.heightAnchor.constraint(equalToConstant: 83.0)
        ]
        
        if #available(iOS 11.0, *) {

           let window = UIApplication.shared.keyWindow
            let bottomPadding: CGFloat = (window?.safeAreaInsets.bottom ?? 0)!
            
            
            selectPhotoButtonConstraints = [
                selectPhotoButton.widthAnchor.constraint(equalToConstant: 44.0),
                selectPhotoButton.heightAnchor.constraint(equalToConstant: 44.0),
                selectPhotoButton.rightAnchor.constraint(equalTo: view.safeAreaLayoutGuide.rightAnchor, constant: -61.5),
                view.safeAreaLayoutGuide.bottomAnchor.constraint(equalTo: selectPhotoButton.bottomAnchor, constant: ((117.0 - bottomPadding) - 44.0) / 2)
            ]
            cancelButtonConstraints = [
                cancelButton.leftAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leftAnchor, constant: 24.0),
                view.safeAreaLayoutGuide.topAnchor.constraint(equalTo: cancelButton.topAnchor, constant: (65.0 / 2) - 20.0)
            ]
            shutterButtonConstraints = [
                shutterButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
                shutterButton.widthAnchor.constraint(equalToConstant: 65.0),
                shutterButton.heightAnchor.constraint(equalToConstant: 65.0),
                view.safeAreaLayoutGuide.bottomAnchor.constraint(equalTo: shutterButton.safeAreaLayoutGuide.bottomAnchor, constant: ((117.0 - bottomPadding) - 65.0) / 2)
            ]
        
        } else {
            
            let window = UIApplication.shared.windows.first
//            let topPadding: CGFloat = window!.safeAreaInsets.top
            let bottomPadding: CGFloat = window!.safeAreaInsets.bottom

            
            selectPhotoButtonConstraints = [
                selectPhotoButton.widthAnchor.constraint(equalToConstant: 44.0),
                selectPhotoButton.heightAnchor.constraint(equalToConstant: 44.0),
                selectPhotoButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -61.5),
                view.bottomAnchor.constraint(equalTo: selectPhotoButton.bottomAnchor, constant:  ((117.0 - bottomPadding) - 44.0) / 2)
            ]
            cancelButtonConstraints = [
                cancelButton.leftAnchor.constraint(equalTo: view.leftAnchor, constant: 24.0),
                view.safeAreaLayoutGuide.topAnchor.constraint(equalTo: cancelButton.topAnchor, constant: (65.0 / 2) - 20.0)
//                view.bottomAnchor.constraint(equalTo: cancelButton.bottomAnchor, constant: (65.0 / 2) - 5.0)
            ]
            shutterButtonConstraints = [
                shutterButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
                shutterButton.widthAnchor.constraint(equalToConstant: 65.0),
                shutterButton.heightAnchor.constraint(equalToConstant: 65.0),
                view.safeAreaLayoutGuide.bottomAnchor.constraint(equalTo: shutterButton.bottomAnchor, constant: ((117.0 - bottomPadding) - 65.0) / 2)
            ]
        }
        NSLayoutConstraint.activate(selectPhotoButtonConstraints + cancelButtonConstraints + shutterButtonConstraints + containerViewConstraints + topContainerViewConstraints)
    }
    
    func imageScannerController(_ scanner: ImageScannerController, didFailWithError error: Error) {
        print(error)
        _result!(nil)
        self.hideButtons()
        self.dismiss(animated: true)
    }
    
    func imageScannerController(_ scanner: ImageScannerController, didFinishScanningWithResults results: ImageScannerResults) {
        // Your ViewController is responsible for dismissing the ImageScannerController
        scanner.dismiss(animated: true)
        self.hideButtons()
        
        let imagePath = saveImage(image:results.doesUserPreferEnhancedScan ? results.enhancedScan!.image : results.croppedScan.image)
        _result!(imagePath)
        self.dismiss(animated: true)
    }
    
    func imageScannerControllerDidCancel(_ scanner: ImageScannerController) {
        // Your ViewController is responsible for dismissing the ImageScannerController
        scanner.dismiss(animated: true)
        self.hideButtons()
        
        _result!(nil)
        self.dismiss(animated: true)
    }
    
    func saveImage(image: UIImage) -> String? {
        
        guard let data = image.jpegData(compressionQuality: 1) ?? image.pngData() else {
            return nil
        }
        
        guard let directory = try? FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false) as NSURL else {
            return nil
        }
        
        let fileName = randomString(length:10);
        let filePath: URL = directory.appendingPathComponent(fileName + ".png")!
        do {
            let fileManager = FileManager.default
            // Check if file exists
            if fileManager.fileExists(atPath: filePath.path) {
                // Delete file
                try fileManager.removeItem(atPath: filePath.path)
            }
            else {
                print("File does not exist")
            }
        }
        catch let error as NSError {
            print("An error took place: \(error)")
        }
        do {
            try data.write(to: filePath)
            return filePath.path
        }
        catch {
            print(error.localizedDescription)
            return nil
        }
    }
    
    func randomString(length: Int) -> String {
        let letters : NSString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let len = UInt32(letters.length)
        var randomString = ""
        for _ in 0 ..< length {
            let rand = arc4random_uniform(len)
            var nextChar = letters.character(at: Int(rand))
            randomString += NSString(characters: &nextChar, length: 1) as String
        }
        return randomString
    }
}
