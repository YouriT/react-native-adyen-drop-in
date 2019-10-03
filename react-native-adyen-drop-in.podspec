require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name            = package['name']
  s.version         = package['version']
  s.summary         = package['description']
  s.description     = package['description']
  s.license         = package['license']
  s.author          = package['author']
  s.homepage        = package['homepage']
  s.source          = { :git => "https://github.com/YouriT/react-native-adyen-drop-in.git", :tag => "v#{s.version}" }

  s.platform        = :ios, '10.3'
  s.swift_version   = '5.0'

  s.preserve_paths  = 'LICENSE', 'README.md'
  s.source_files    = "ios/**/*.{h,m,swift}"

  s.dependency 'React'
  s.dependency 'Adyen','~> 3.1.3'
end
  