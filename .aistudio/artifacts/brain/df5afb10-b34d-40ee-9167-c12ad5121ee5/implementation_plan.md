# Admin Paneldə Sifarişlər, Bildiriş Filtrləməsi və Müştəri Xidmətləri Çatı

Bu təkmilləşdirmə admin hesabında (`elmarmmdov1@gmail.com` və digər adminlər) müştəri sifarişlərinin dərhal görünməsini, admin tərəfindən müştərilərə telefonun SMS tətbiqi və ya WhatsApp vasitəsilə mesaj göndərilməsini, admin bildirişlərinin müştəri bildirişlərindən tam ayrılaraq yalnız adminə aid hadisələri (yeni sifariş, müştəri mesajları, status dəyişiklikləri) göstərməsini və müştərilərin Çat bölməsindən birbaşa "BestGroup.az Müştəri Xidmətləri" ilə əlaqə saxlamasını təmin edir.

## User Review & Critical Decisions

> [!IMPORTANT]
> Təsdiqlənmiş qərarlar:
> - **SMS və Mesajlaşma Yolu**: Telefonun SMS tətbiqi (`smsto:`) və ya WhatsApp vasitəsilə hazır şablonlu və fərdi mesaj açılışı.
> - **Admin İcazələri**: `elmarmmdov1@gmail.com` baş admin, həmçinin Firestore `users` kolleksiyasında `role == 'admin'` və ya `admins` siyahısındakı digər adminlər.
> - **Admin Bildirişlərinin Ayrılması**: Müştərinin şəxsi sifariş statusu bildirişləri admin bildiriş panelində göstərilməyəcək; admin panelində yalnız inzibati hadisələr (yeni sifariş daxil oldu, yeni müştəri mesajı gəldi, sifariş statusu yeniləndi) əks olunacaq.
> - **Müştəri Xidmətləri Çatı**: Başlıq **"BestGroup.az Müştəri Xidmətləri"** olaraq təqdim ediləcək və sifarişi olmayan müştəri belə daxil olub birbaşa baş adminlə əlaqə saxlaya biləcək.

## 1. Overview & Core Concept

- **Nə edir**:
  - Müştəri sifariş verdikdə bu sifariş dərhal admin paneldə ("Dashboard" və "Sifarişlər" tablarında) canlı əks olunur.
  - Admin istənilən sifariş və ya müştəri üzərindən "SMS / WhatsApp Göndər" düyməsinə toxunaraq birbaşa telefonun mesajlaşma tətbiqini və ya WhatsApp-ı açır.
  - Bildiriş mərkəzində rol bölgüsü tətbiq edilir: müştərinin özünə gedən "Sifarişiniz qeydə alındı" tipli bildirişlər adminə göstərilmir; admin üçün "Yeni sifariş daxil oldu: #BG-...", "Yeni dəstək müraciəti" kimi inzibati bildirişlər generasiya olunur və ya süzülür.
  - Müştərinin aşağı menyusundakı "Çat" bölməsi açıldıqda, müştəri birbaşa "BestGroup.az Müştəri Xidmətləri" dialoquna daxil olur və yazışma baş adminin admin panelindəki "Müştəri xidmətləri" tabına düşür.
- **Hədəf İstifadəçilər**: Sistem adminləri (`elmarmmdov1@gmail.com`), kuratorlar və tələbə müştərilər.
- **Əsas Dəyər**: İnzibati səliqə, təmiz bildiriş paneli, operativ SMS/WhatsApp əlaqəsi və 24/7 birbaşa müştəri xidmətləri çıxışı.

## 2. User Experience & Visual Design

- **Admin Sifarişlərin İdarəsi və Əlaqə**:
  - Sifariş kartlarında və sifariş detalında xüsusi **"SMS / WhatsApp"** düyməsi (`Gold500` və `Navy800` rəng harmoniyası ilə).
  - Açılan `ContactCustomerBottomSheet`:
    - Müştərinin adı və telefon nömrəsi
    - Sürətli şablon çipləri ("Sifarişiniz təsdiqləndi", "Əlavə məlumat lazımdır", "Sifarişiniz hazırdır")
    - Redaktə edilə bilən mətn sahəsi
    - İki güclü hərəkət düyməsi: **"SMS ilə aç"** və **"WhatsApp-da aç"**
- **Admin Bildiriş Paneli (`AdminNotificationsContent`)**:
  - Yalnız inzibati kateqoriyalı bildirişlər: Yeni daxil olan sifarişlər, Müştəri müraciətləri, Sistem və status dəyişiklikləri.
  - Müştərinin fərdi "sifarişiniz qeydə alındı / statusunuz dəyişdi" bildirişləri admin siyahısından təmizlənir.
- **Müştəri Tərəfi Çat Ekranı (`ChatScreen`)**:
  - Başlıq: **"BestGroup.az Müştəri Xidmətləri"**
  - Alt yazı: *"Baş admin və kuratorluq xidməti aktivdir"*
  - İlkin vəziyyətdə (heç bir sifariş olmasa belə) xoşgəlmisiniz mətni, sürətli sual çipləri və dərhal yazmağa hazır aktiv mətn sahəsi.
  - Göndərilən mesaj birbaşa Firestore `conversations` kolleksiyasında dəstək dialoqu kimi formalaşır və admin tərəfində dərhal bildirişlə çıxır.

## 3. Key Product Decisions & Trade-Offs

- **Bildirişlərin İkili Rol Filtri (`NotificationItem.recipientRole` & `targetUserId`)**:
  - *Seçilmiş Yanaşma*: Bildiriş yaradılarkən hədəf kütlə qeyd edilir: müştəri bildirişləri `recipientRole = "customer"` və `targetUserId = customerId`, admin bildirişləri isə `recipientRole = "admin"` və ya `isAdminNotification = true`.
  - *Səbəb*: Admin müştərinin özünə aid bildirişləri görməməli, yalnız müştərinin verdiyi yeni sifariş barədə inzibati xəbərdarlıq almalıdır.
- **Birbaşa Müştəri Xidmətləri Dialoqu**:
  - *Seçilmiş Yanaşma*: Müştəri `Chat` tabını seçdikdə `getOrCreateSupportConversation()` avtomatik olaraq "BestGroup.az Müştəri Xidmətləri" başlığı ilə həmin müştərinin dialoqunu açır və ya aktivləşdirir.
  - *Səbəb*: Müştərinin əvvəlcədən sifarişinin olub-olmamasından asılı olmayaraq baş adminlə birbaşa əlaqəsini təmin edir.

## 4. Technical Architecture & Data Strategy

### Arxitektura və Məlumat Axını Diaqramı

```
┌────────────────────────────────────────────────────────────────────────┐
│                           Firestore Backend                            │
│  ┌──────────────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│  │   /orders/{orderId}  │  │   /conversations/   │  │ /notifications│  │
│  │  - customerName      │  │  - title: "BestGroup│  │ - targetRole  │  │
│  │  - customerPhone     │  │    .az Müştəri Xid."│  │   ("admin" vs │  │
│  │  - status, isoNow    │  │  - isSupport: true  │  │    "customer")│  │
│  └──────────┬───────────┘  └──────────┬──────────┘  └───────┬───────┘  │
└─────────────┼─────────────────────────┼─────────────────────┼──────────┘
              │ Real-time Stream        │ Real-time Stream    │ Role Filter
              ▼                         ▼                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     BestGroupRepository & Services                     │
│  - isStaff: email == "elmarmmdov1@gmail.com" || role == "admin"       │
│  - getOrdersFlow(userId, isStaff = true) -> All orders to admin        │
│  - getAdminNotificationsFlow() -> Filter out customer-only notices     │
│  - getOrCreateSupportConversation() -> "BestGroup.az Müştəri Xidmətləri"│
└─────────────────────────────┬──────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌───────────────────────────┐   ┌───────────────────────────┐
│     Admin Panel Flow      │   │    Customer Chat Flow     │
│ - Live Orders List        │   │ - "BestGroup.az Müştəri   │
│ - SMS / WhatsApp Intent   │   │    Xidmətləri" Dialoqu    │
│ - Pure Admin Notifications│   │ - No order needed to chat │
└───────────────────────────┘   └───────────────────────────┘
```

### Həyata Keçiriləcək Addımlar:
1. **Model və Bildirişlər Layeri (`NotificationItem.kt`, `BestGroupRepository.kt`)**:
   - `NotificationItem`-ə `targetRole` (və ya `forAdmin: Boolean`) və `targetUserId` dəstəyi əlavə etmək.
   - Müştəri sifariş verdikdə iki fərqli bildiriş yaratmaq:
     - Müştəri üçün: "Sifarişiniz qəbul edildi" (`targetRole = "customer"`, `targetUserId = userId`)
     - Admin üçün: "Yeni sifariş daxil oldu: #BG-..." (`targetRole = "admin"`)
   - Admin bildiriş siyahısında müştəri bildirişlərini tam süzgəcdən keçirmək.
2. **Sifarişlər və Əlaqə (`FirestoreOrderService.kt`, `AdminDashboardScreen.kt`, `OrderDetailScreen.kt`)**:
   - Sifariş yaradılarkən müştərinin adı və telefonu sifariş sənədinə yazılır.
   - `AdminDashboardScreen.kt`-də `ContactCustomerBottomSheet` və hər sifariş kartına/müştəri kartına SMS və WhatsApp düyməsi yerləşdirmək.
3. **Müştəri Xidmətləri Çatı (`ChatScreen.kt`, `BestGroupRepository.kt`, `FirestoreChatService.kt`)**:
   - Başlıq dəqiq "BestGroup.az Müştəri Xidmətləri" olaraq təyin ediləcək.
   - İstifadəçi Çat bölməsinə keçəndə avtomatik baş adminlə dəstək söhbəti aktivləşdiriləcək.
4. **Kompilyasiya və Təsdiq**:
   - `compile_applet` ilə tətbiqin qüsursuz işləməsini yoxlamaq.
