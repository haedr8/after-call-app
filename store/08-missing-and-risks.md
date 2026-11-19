# ما ينقص وما يهدد القبول (الحالة)

## جاهز
- الأيقونة: store/graphics/icon-512.png
- صورة الغلاف: store/graphics/feature-1024x500.png
- سياسة الخصوصية مع بريدك: privacy-play.html (لازم تُنشر برابط عام، انظر أدناه)
- نصوص المتجر 01/02، الأذونات 03، النماذج 04، المراجعة 06

## يحتاج منك
1. **لقطات شاشة (2 على الأقل، 4 أفضل)** من هاتفك: شاشة التطبيق الرئيسية، لوحة الأرقام، شاشة المكالمة، نافذة الخيارات، السجل. صوّرها بعد ما تتأكد أنها لا تحتوي أرقام زبائن حقيقية.
2. **رابط سياسة الخصوصية:** الصفحة privacy-play.html لازم تنزل على GitHub Pages (فرع main) ليصير الرابط: https://haedr8.github.io/after-call-app/privacy-play.html
3. **تجربة المكالمات** على هاتفين (سامسونك + تكنو/إنفينكس): واردة، صادرة، رفض، كتم، سبيكر، بلوتوث، مكالمتين، رقم طوارئ.
4. **اختبار مغلق:** 12 مختبراً × 14 يوم متواصلة (حساب شخصي).
5. **تعبئة النماذج** (انسخ من الملفات): Data safety، Content rating، Target audience، App access، Ads.

## نموذج صلاحيات سجل المكالمات
- نوع: Default Phone handler. النص بملف 03.

## نموذج USE_FULL_SCREEN_INTENT (إذا طلبته كوكل)
- Use case: Phone calls (incoming call screen).
- English: The app is a default phone handler. It uses a full-screen intent only to show the incoming call screen over the lock screen, as the system dialer does. It is not used for ads or promotions.

## تبرير READ_CONTACTS
The app is a phone app: it shows the contact list with search and the caller's name on incoming, outgoing and past calls. Contacts are never sent anywhere; the app has no internet permission.

## تبرير CALL_PHONE
Used to place calls from the dialpad, call log, contacts and the options dialog.

## Content rating (IARC): كل الإجابات «لا». الفئة: Utility. Target audience: 18+.

## مخاطر معروفة
- مراجعة يدوية لتطبيق يطلب سجل المكالمات؛ استعد لنص الاستئناف (06).
- أي عطل بالمكالمات يرفع احتمال الرفض أو الإزالة لاحقاً.
- المكالمات الطارئة تمر عبر نظام الهاتف، لكن جرّب 112 فقط بتأكيد وإلغاء سريع إن أمكن، أو اتركها.
