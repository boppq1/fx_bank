import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:image_picker/image_picker.dart';

void main() async {
  // 플러터 엔진과 네이티브 플랫폼 간의 바인딩을 보장하는 필수 코드
  WidgetsFlutterBinding.ensureInitialized(); 
  
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BNK Bank Event',
      debugShowCheckedModeBanner: false,
      home: const WebScreen(),
    );
  }
}

class WebScreen extends StatefulWidget {
  const WebScreen({super.key});

  @override
  State<WebScreen> createState() => _WebScreenState();
}

class _WebScreenState extends State<WebScreen> {
  late final WebViewController _controller;
  final ImagePicker _picker = ImagePicker();

  @override
  void initState() {
    super.initState();

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setUserAgent("Flutter/fx_bank") // 스프링 백엔드가 앱 접속인지 판단하는 기준이 됩니다!
      ..setOnConsoleMessage((JavaScriptConsoleMessage message) {
    debugPrint('WebView 콘솔: ${message.message}');
  })
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageFinished: (String url) => debugPrint('로딩 완료: $url'),
        ),
      )
      ..addJavaScriptChannel(
        'FlutterEventBridge',
        onMessageReceived: (JavaScriptMessage message) {
          final Map<String, dynamic> data = jsonDecode(message.message);
          if (data['action'] == 'openCamera') {
            _takePictureAndUpload(data['letter']);
          }
        },
      )
      // ⚠️ 중요: 현재 구동 중인 본인의 스프링 서버 주소(IP)를 적어주세요.
      // 안드로이드 에뮬레이터에서 내 컴퓨터(localhost)를 가리키는 주소는 10.0.2.2 입니다.
      ..loadRequest(Uri.parse('http://localhost:8080/')); 
  }

  Future<void> _takePictureAndUpload(String letter) async {
    try {
      final XFile? photo = await _picker.pickImage(source: ImageSource.camera);
      if (photo != null) {
        File imageFile = File(photo.path);
        List<int> imageBytes = await imageFile.readAsBytes();
        String base64Image = base64Encode(imageBytes);

        // 사진 촬영이 끝나면 웹 브라우저의 window.onCameraResult 함수를 강제로 호출하면서 데이터를 넘깁니다.
        _controller.runJavaScript('window.onCameraResult("$letter", "$base64Image");');
      }
    } catch (e) {
      debugPrint("카메라 에러: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('BNK 뱅크 이벤트'),
        backgroundColor: Colors.blue,
      ),
      body: WebViewWidget(controller: _controller),
    );
  }
}