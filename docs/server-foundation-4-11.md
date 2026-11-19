# زیرساخت گزینه‌های ۴ و ۱۱

## چت با فروشگاه

صفحه «چت با فروشگاه» در اپ مشتری اضافه شده و قراردادهای `ChatConversation` و `ChatMessage` در shared-core قرار گرفته‌اند. نسخه فعلی عمداً بدون شبکه کار می‌کند و پیام‌ها را فقط در حالت نمایشی همان صفحه نشان می‌دهد.

API پیشنهادی:

- `POST /v1/support/conversations`
- `GET /v1/support/conversations/{id}/messages`
- `POST /v1/support/conversations/{id}/messages`
- `POST /v1/support/conversations/{id}/read`

## اعلان حرفه‌ای

صندوق «اعلان‌های فروشگاه» به حساب مشتری اضافه شده و صف ساخت اعلان در پنل مدیریت حفظ شده است. برای اعلان بالای گوشی باید بعداً FCM یا سرویس Push روی سرور فعال شود.

API پیشنهادی:

- `POST /v1/devices/register`
- `GET /v1/notifications`
- `POST /v1/notifications/{id}/read`
- `POST /v1/admin/notifications`

کلید سرویس Push نباید در APK یا GitHub قرار بگیرد.
