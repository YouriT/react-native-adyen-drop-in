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

  s.platform        = :ios, '11.0'
  s.swift_version   = '5.0'

  s.preserve_paths  = 'LICENSE', 'README.md'
  s.source_files    = "ios/**/*.{h,m,swift}"

  s.dependency 'React-Core'
  s.dependency 'Adyen', '4.4.0'
end
  