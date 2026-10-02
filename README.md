# 🎨 ColorStory (Happy Color Narrative Edition)

لعبة تلوين بالأرقام (Color by Number) احترافية مبنية بلغة **Kotlin** لنظام أندرويد، مدمجة بنظام قصص وفصول سردية (Narrative Chapters).

---

## 🌟 مميزات اللعبة

1. **نواة لعب سريعة وسلسة (60–120 FPS):**
   * تقريب وتحريك فائق السلاسة (Pinch-to-zoom & Pan) حتى 15x دون فقدان الدقة.
   * استجابة لمس فورية $O(1)$ باستخدام خريطة البكسلات الذكية (ID Buffer).
2. **تمييز الأرقام الذكي (Active Number Highlights):**
   * عند اختيار أي لون، تظهر دوائر بيضاء واضحة تبرز القطع المطلوب تلوينها (نفس نظام Happy Color).
3. **نظام المساعدة (Hint System):**
   * زر المصباح ينتقل تلقائياً بالكاميرا إلى أكبر قطعة غير ملونة في اللون المختار ويطلق نبضة ضوئية لتنبيه اللاعب.
4. **نظام الفصول والقصص (Story Progression):**
   * فصول متعددة وقصة تتكشف أحداثها مع كل لوحة تكتمل.
   * نافذة قصة تمهيدية وخاتمة عند الفوز مع مكافأة فتح المستوى التالي.
5. **خط إنتاج تحويل الصور (AI to Level Converter):**
   * سكريبت بايثون متكامل في مجلد `tools/` لتحويل أي صورة مولدة بالذكاء الاصطناعي إلى مرحلة مكتملة في اللعبة تلقائياً.

---

## 🚀 طريقة البناء التلقائي عبر GitHub Actions (بدون كمبيوتر)

بمجرد رفع الملفات على مستودع GitHub، سيبدأ سير العمل (`.github/workflows/android.yml`) في بناء التطبيق تلقائياً:

1. ادخل على مستودعك: `https://github.com/mostaffa2010/color-game`
2. اضغط على تبويب **Actions**.
3. ستجد مهمة البناء تعمل (تستغرق حوالي دقيقتين).
4. بعد اكتمال البناء باللون الأخضر، اضغط عليها وحمّل ملف:
   **`ColorStory-Debug-APK`** أو من قسم **Releases**.
5. قم بتثبيت ملف الـ APK مباشرة على هاتفك واستمتع باللعبة!

---

## 📱 أوامر Termux لرفع المشروع على GitHub

افتح تطبيق **Termux** على هاتفك ونفذ الأوامر التالية بالترتيب:

```bash
# 1. الدخول إلى مجلد المشروع (بعد فك الضغط في الذاكرة)
cd color-game

# 2. تهيئة المستودع
git init
git config user.name "Mostafa"
git config user.email "mostaffa201021@gmail.com"

# 3. إضافة جميع الملفات وعمل Commit
git add .
git commit -m "Initial commit: Complete ColorStory Android Kotlin game with Chapter 1 Bakery level"

# 4. ربط المستودع بالفرع الرئيسي
git branch -M main
git remote add origin https://github.com/mostaffa2010/color-game.git

# 5. الرفع إلى GitHub
git push -u origin main
```
*(ملاحظة: سيطلب منك GitHub كتابة اسم المستخدم، وعند طلب كلمة المرور أدخل **Personal Access Token** الخاص بحسابك على GitHub).*

---

## 🛠️ كيفية إضافة صور ومستويات جديدة مستقبلاً

عندما تولد أي صورة جديدة بالذكاء الاصطناعي (مثل مخبز، برج الساعة، غابة):
نفذ الأمر:
```bash
python3 tools/convert_image_to_level.py --image your_image.jpg --outdir app/src/main/assets/levels/level_name --title "Title" --chapter 1
```
ثم أضف المستوى إلى `levels_manifest.json` وارفعه!
