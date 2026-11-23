# Permissions Declaration / Data safety (نسخة تطبيق الهاتف)

## الدور
التطبيق «تطبيق الهاتف الافتراضي» (ROLE_DIALER): لوحة أرقام، سجل، استقبال/إنهاء مكالمات عبر InCallService.

## الصلاحيات
| الإذن | السبب |
|---|---|
| READ_CALL_LOG | عرض سجل المكالمات داخل التطبيق (وظيفة أساسية لتطبيق الهاتف) |
| READ_CONTACTS | عرض اسم المتصل |
| CALL_PHONE | إجراء المكالمات من لوحة الأرقام والسجل |
| READ_PHONE_STATE | حالة المكالمة |
| USE_FULL_SCREEN_INTENT | عرض شاشة المكالمة الواردة (استخدام أساسي لتطبيق اتصال) |
| POST_NOTIFICATIONS, WAKE_LOCK | إشعار المكالمة وإطفاء الشاشة قرب الأذن |
| ACCESS_COARSE/FINE_LOCATION | عند ضغط «موقعي» فقط |

لا يوجد: INTERNET, SMS, CAMERA, SYSTEM_ALERT_WINDOW, REQUEST_INSTALL_PACKAGES, قراءة الرسائل.

## Permissions Declaration Form (Call Log)
- اختر: **Default Phone handler**.
- English justification: The app is a full default phone handler (ROLE_DIALER) with dialer UI and InCallService. READ_CALL_LOG is used only to show the in-app call history. No data is transmitted; the app has no INTERNET permission.

## Data safety
- Collect/share any data: **No** (لا INTERNET، لا يغادر شي الجهاز).

## Prominent disclosure
موجود بالتطبيق قبل طلب الدور والصلاحيات (زر «موافق»/«ليس الآن»).

## WRITE_CONTACTS (يُطلب فقط عند ضغط «حذف جهة الاتصال»)
English: Used only when the user explicitly deletes a contact from the in-app contact list. The user confirms each deletion; nothing is sent anywhere.
