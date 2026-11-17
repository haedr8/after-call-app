# ملاحظات للمراجعة (App access / Notes to reviewer) – إنجليزي
The app needs no login. To test: grant Phone state and Notifications on first launch, make a short call from the phone dialer and end it; a "Call ended" notification appears. Tap it to see quick-reply templates; choosing one opens WhatsApp with the text. Number is not read automatically (no call-log permission): use "Enter number" or "Contacts" (system picker). No data leaves the device.

# نص استئناف إن رُفض (Appeal)
Hello, our app Haulni (com.haulni.app) does not request call-log, SMS, contacts, overlay or install permissions. It uses READ_PHONE_STATE only to detect call end and show a local notification, with an in-app prominent disclosure before the request. It has no network permission, so it cannot transmit data. Please re-review and tell us the specific policy section if you still find an issue.

# تنبيه صريح للمطوّر
- قد يطلب Google تبرير READ_PHONE_STATE؛ النص أعلاه جاهز.
- الإشعار قد يظهر أيضاً لمكالمات واتساب أحياناً (لأنه لا يقرأ السجل).
- لا يظهر رقم الزبون تلقائياً؛ يُدخل يدوياً أو من جهات الاتصال.
- على هواتف تكنو/إنفينكس قد لا يصل الإشعار بدون خدمة دائمة (محذوفة لأنها غير مقبولة).
