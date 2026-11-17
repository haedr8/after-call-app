# Permissions Declaration / Data safety

## الصلاحيات في النسخة (4 فقط)
| الإذن | السبب |
|---|---|
| READ_PHONE_STATE | معرفة بداية ونهاية المكالمة لإظهار الإشعار. لا يقرأ رقماً ولا سجلاً |
| POST_NOTIFICATIONS | إظهار إشعار «انتهت المكالمة» |
| ACCESS_COARSE/FINE_LOCATION | فقط عند ضغط المستخدم «موقعي» لإرسال موقعه بواتساب (إذن وقت الاستخدام، يُطلب عند الضغط) |

**غير موجودة:** READ_CALL_LOG, READ_CONTACTS, CALL_PHONE, SYSTEM_ALERT_WINDOW, REQUEST_INSTALL_PACKAGES, INTERNET, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE.
لذلك لا يحتاج «Permissions Declaration Form» لسجل المكالمات.

## Data safety (نموذج بيانات المتجر)
- Does your app collect or share any required user data types? **No**
- Is all of the user data collected by your app encrypted in transit? غير منطبق (لا يوجد جمع)
- Data deletion: غير منطبق؛ البيانات محلية وتُحذف بحذف التطبيق.
- Privacy policy URL: رابط صفحة privacy.html (انظر 05)

## Prominent disclosure
موجود داخل التطبيق قبل طلب الإذن: شاشة «إشعار بانتهاء المكالمة» بزر موافق / ليس الآن.

## وصف استخدام READ_PHONE_STATE (إنجليزي للنموذج)
The app listens to the system phone-state broadcast only to know when a phone call ends, so it can show a notification with quick-reply WhatsApp templates. It does not read the phone number, call log, or contacts, and it does not transmit any data.

## Foreground/background location
لا يستخدم موقعاً بالخلفية؛ يُقرأ الموقع مرة واحدة عند ضغط المستخدم.
