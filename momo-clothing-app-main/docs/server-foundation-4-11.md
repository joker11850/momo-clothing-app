# زیرساخت گزینه ۴: اعلان حرفه‌ای

صندوق «اعلان‌های فروشگاه» در اپ مشتری و صف ساخت اعلان در پنل مدیریت حفظ شده است. برای اعلان بالای گوشی باید بعداً FCM یا سرویس Push روی سرور فعال شود.

API پیشنهادی:

- `POST /v1/devices/register`
- `GET /v1/notifications`
- `POST /v1/notifications/{id}/read`
- `POST /v1/admin/notifications`

کلید سرویس Push نباید در APK یا GitHub قرار بگیرد.
