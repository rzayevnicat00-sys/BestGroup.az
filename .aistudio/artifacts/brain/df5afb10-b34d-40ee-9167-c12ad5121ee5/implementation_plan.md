# Admin Çat İdarəsi, Sifariş Sinxronizasiyası və Qalıcı Tema Planı

Bu təkmilləşdirmə admin hesabında (`elmarmmdov1@gmail.com`) müştəri sifarişlərinin dərhal və etibarlı şəkildə görünməsini təmin edir, Firestore təhlükəsizlik qaydalarındakı xətanı aradan qaldırır, admin panelində sifariş vermək və ya yeni çat yaratmaq düymələrini tam ləğv edərək yalnız daxil olan müştəri mesajlarını əks etdirir və adminin həmin dialoq daxilində birbaşa cavab yazmasına şərait yaradır. Həmçinin həm müştəri, həm də admin üçün seçilmiş tünd/işıqlı görünüş temasının daimi saxlanılmasını zəmanətləndirir.

## User Review & Critical Decisions

> [!IMPORTANT]
> Təsdiqlənmiş qərarlar:
> - **Sifariş və Yeni Çat Düymələrinin Ləğvi**: Admin hesabında sifariş yaratmaq və ya yeni çat açmaq funksiyaları tamamilə gizlədilir; sifariş vermək sırf müştərilərə aiddir.
> - **Müştəri Mesajlarına Birbaşa Cavab**: Admin panelinin "Müştəri xidmətləri" bölməsində yalnız müştərilərin baş adminə/dəstəyə yazdığı söhbətlər göstərilir. Admin hər hansı müştərini seçdikdə admin panelinin daxilində tam çat pəncərəsi açılır və admin birbaşa oradan cavab yazır.
> - **Sifarişlərin Admin Paneldə Canlı Görünməsi**: Firestore qaydalarındakı `isSuperAdmin()` yoxlaması düzəldilərək `elmarmmdov1@gmail.com` üçün sifariş və çat axınlarına tam oxuma/yazma hüququ təmin edilir.
> - **Temanın Dəyişməz Qalması**: İstər müştəri, istərsə də admin tünd və ya işıqlı temanı seçdikdə, tətbiqdən çıxıb yenidən daxil olduqda seçilmiş tema sıfırlanmadan qorunur.

## 1. Overview & Core Concept

- **Nə edir**:
  - Müştəri sifariş göndərdikdə sifariş dərhal admin panelindəki "Dashboard" və "Sifarişlər" bölmələrində görünür.
  - Müştəri sifariş verməsə belə, "Çat" bölməsindən "BestGroup.az Müştəri Xidmətləri"nə yazdığı mesajlar dərhal admin panelinin "Müştəri xidmətləri" bölməsinə düşür.
  - Admin panelində sifariş vermək və ya yeni çat açmaq seçimləri tam yığışdırılır; admin yalnız müştərilərin müraciətlərini qəbul edir və cavablandırır.
  - Admin panel daxilində inteqrasiya olunmuş `AdminSupportChatDetailView` açılır, admin müştərinin yazdığı sualları oxuyur və birbaşa cavab göndərir.
  - Həm admin profilində, həm də müştəri profilində seçilən tema `SharedPreferences` vasitəsilə bərpa olunur və sıfırlanmır.
- **Hədəf İstifadəçilər**: Sistem idarəçisi (`elmarmmdov1@gmail.com`) və sifarişçi tələbələr.
- **Əsas Dəyər**: İnzibati səlahiyyətlərin dəqiq ayrılması, sifarişlərin fasiləsiz sinxronizasiyası və təmiz müştəri xidməti iş axını.

## 2. User Experience & Visual Design

- **Admin Müştəri Xidmətləri Çatı (`AdminSupportContent` & `AdminSupportChatDetailView`)**:
  - **Söhbət Siyahısı**: Yalnız müştərilərin Müştəri Xidmətlərinə yazdığı dialoqlar (`orderId == null`). Müştərinin adı, telefon nömrəsi, son mesajı və oxunmamış mesaj sayı əks olunur.
  - **Dialoqa Daxil Olma**: Söhbətə toxunduqda admin paneldən çıxmadan daxili çat ekranı açılır:
    - Yuxarı zolaq: Geri qayıtma oxu, müştərinin adı, universiteti və "Aktiv Müştəri" statusu.
    - Mesajlar lenti: Müştərinin göndərdiyi mesajlar (sola düzlənmiş) və adminin göndərdiyi cavablar (sağa düzlənmiş, qızılı/tünd göy tonlarda).
    - Mesaj yazma zolağı: "Müştəriyə cavab yazın..." və Göndər düyməsi.
  - **Artıq Düymələrin Yığışdırılması**: Heç bir "Yeni çat yarat" və ya "Sifariş ver" düyməsi olmayacaq.
- **Admin Paneldə Sifarişlər Paneli**:
  - Ən son sifarişlər ən üstdə olmaqla canlı yenilənən siyahı.
  - Müştərinin adı, telefonu və sifariş statusları ilə birbaşa əlaqə.
- **Qalıcı Görünüş Teması**:
  - Admin panelinin "Profil" tabında və müştərinin Profil ekranında seçilmiş işıqlı və ya tünd tema tətbiq bağlanıb açıldıqda dərhal bərpa olunur.

## 3. Key Product Decisions & Trade-Offs

- **Firestore Qaydalarının Təkmilləşdirilməsi (`firestore.rules`)**:
  - *Seçilmiş Yanaşma*: `isSuperAdmin()` funksiyasında dəstəklənməyən `.lower()` metodunu aradan qaldırmaq, dəqiq və təhlükəsiz regex/bərabərlik (`request.auth.token.email.matches('(?i)^elmarmmdov1@gmail[.]com$')`) tətbiq etmək və qaydaları dərhal Firebase-ə deploy etmək.
  - *Səbəb*: `.lower()` metodu Firestore qaydalarında sintaksis xətası yaradaraq `PERMISSION_DENIED` verirdi və bu səbəbdən admin sifarişləri görə bilmirdi.
- **Admin Panel Daxili Çat Dialoqu**:
  - *Seçilmiş Yanaşma*: Müştəri söhbətini seçdikdə müştəri ekranlarına keçmək əvəzinə, admin panelin öz içərisində `AdminSupportChatDetailView` komponenti ilə dialoqu açmaq.
  - *Səbəb*: Admin müştəri tətbiqinin navigasiyasına keçmir, sifariş vermək və ya yeni çat açmaq kimi müştəriyə aid düymələri görmür, sırf operator/baş admin rolunda cavab yazır.

## 4. Technical Architecture & Data Strategy

### Arxitektura və Məlumat Axını Diaqramı

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Firestore Cloud Backend                         │
│  ┌──────────────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│  │   /orders/{orderId}  │  │   /conversations/   │  │ /users/{uid}  │  │
│  │  - userId            │  │  - customerId       │  │ - role: admin │  │
│  │  - customerName      │  │  - orderId: null    │  └───────────────┘  │
│  │  - status, createdAt │  │  - /messages/...    │                     │
│  └──────────┬───────────┘  └──────────┬──────────┘                     │
└─────────────┼─────────────────────────┼────────────────────────────────┘
              │ Real-time Stream        │ Real-time Stream
              ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  BestGroupRepository & Services                        │
│  - isStaff check: email == "elmarmmdov1@gmail.com" || role == "admin"  │
│  - getOrdersFlow(isStaff = true) -> Real-time orders to Admin Panel    │
│  - getConversationsFlow() -> Support conversations to Admin Panel      │
│  - sharedPrefs.getString("theme_mode") -> Restores theme on startup    │
└─────────────────────────────┬──────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌───────────────────────────┐   ┌───────────────────────────┐
│     Admin Panel Flow      │   │    Customer App Flow      │
│ - Live Orders List        │   │ - "Sifariş ver" aktivdir  │
│ - NO "Sifariş ver"        │   │ - Sifarişsiz belə Müştəri │
│ - NO "Yeni çat yarat"     │   │   Xidmətlərinə yaza bilir │
│ - Customer Support Chats: │   │ - Seçilən tema dəyişməz   │
│   * AdminSupportChatDetail│   │   qalıcı qalır            │
│   * Direct reply to user  │   └───────────────────────────┘
└───────────────────────────┘
```

### Həyata Keçiriləcək Dəqiq Addımlar:
1. **Firestore Qaydalarının Düzəldilməsi və Deploy Edilməsi (`firestore.rules`)**:
   - `isSuperAdmin()` funksiyasında `.lower()` xətasını aradan qaldırmaq və regex bərabərliyi tətbiq etmək.
   - Qaydaları `deploy_firebase` aləti ilə dərhal canlı bazaya göndərmək.
2. **Admin Çat İnteqrasiyası (`AdminDashboardScreen.kt`)**:
   - "Müştəri xidmətləri" bölməsində dialoqa toxunduqda birbaşa admin panel daxilində `AdminSupportChatDetailView` açmaq.
   - Dialoq daxilində mesaj axını, oxunma və birbaşa cavab göndərmə imkanı təqdim etmək.
   - Bütün lazımsız düymələri ("Sifariş ver", "Yeni çat yarat") admindən tamamilə təmizləmək.
3. **Müştəri Tərəfində Sifarişsiz Dəstək və Rol Ayrımı (`HomeScreen.kt`, `OrdersScreen.kt`)**:
   - Müştərilər sifariş yaratmasalar belə birbaşa baş adminlə əlaqə saxlaya biləcəklər.
   - Əgər daxil olan istifadəçi admindirsə (`isStaff == true`), müştəri ekranlarındakı "Sifariş ver" FAB düymələri gizlədilir.
4. **Temanın Qalıcılığının Yoxlanılması**:
   - `BestGroupRepository` və `MainActivity`-də temanın həm admin, həm də müştəri üçün `SharedPreferences`-dən düzgün oxunub saxlandığını təsdiqləmək.
5. **Kompilyasiya və Sınaq**:
   - `compile_applet` ilə tətbiqin qüsursuz işləməsini yoxlamaq.
