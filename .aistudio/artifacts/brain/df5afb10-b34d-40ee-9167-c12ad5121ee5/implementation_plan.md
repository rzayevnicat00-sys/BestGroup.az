# Admin Paneldə Müştəri Sifarişləri və SMS/WhatsApp Əlaqə Sistemi

Bu təkmilləşdirmə müştərilər sifariş verdikdə həmin sifarişlərin dərhal admin panelində canlı görünməsini təmin edir, `elmarmmdov1@gmail.com` və onun təyin etdiyi adminlərə tam idarəetmə səlahiyyəti verir və adminin müştəri ilə bir toxunuşla telefonun SMS tətbiqi və ya WhatsApp vasitəsilə əlaqə saxlamasına imkan yaradır.

## User Review & Critical Decisions

> [!IMPORTANT]
> İstifadəçi tərəfindən verilmiş aydınlaşdırıcı cavablar əsasında qərarlar təsdiqlənmişdir:
> - **SMS və Mesajlaşma Yolu**: Telefonun standart SMS tətbiqi və WhatsApp tətbiqi vasitəsilə birbaşa açılış (`Intent.ACTION_SENDTO` və WhatsApp API linki).
> - **Admin İcazələri**: `elmarmmdov1@gmail.com` baş admin təyin edilir, Firestore qaydalarında və auth xidmətində həmin email və onun təyin etdiyi adminlər bütün sifarişləri oxumaq/yeniləmək səlahiyyəti əldə edir.
> - **Sifarişlərin Sıralanması və İdarəsi**: Canlı (real-time Firestore snapshot listener) yenilənən tam siyahı, vaxta görə ən yenilər yuxarıda olmaqla və status filterləri (Gözləyir, Qəbul edildi, Hazırlanır, Hazırdır, İmtina).

## 1. Overview & Core Concept

- **Nə edir**: 
  - Müştəri mobil tətbiqdən sifariş yerləşdirdikdə, admin paneldəki sifariş siyahısı və dashboard statistikası anında avtomatik yenilənir.
  - Sifariş detallarında və idarəetmə kartında müştərinin əlaqə nömrəsi, adı və universitet məlumatları aydın şəkildə əks olunur.
  - Admin istənilən sifariş üzrə və ya müştərilər bölməsindən "SMS / WhatsApp Göndər" düyməsini sıxaraq hazır şablonlu və ya fərdi mesajla cihazın SMS tətbiqinə və ya WhatsApp-a keçid edir.
- **Hədəf İstifadəçi**: Sistem adminləri (`elmarmmdov1@gmail.com`), sifariş kuratorları və xidmət menecerləri.
- **Əsas Dəyər**: Müştəri sifarişlərinin itməməsi, dərhal baxılması, operativ əks-əlaqə və sürətli müştəri məmnuniyyəti.

## 2. User Experience & Visual Design

- **Admin Sifarişlər Tabı (`AdminOrdersContent`)**:
  - Üst hissədə horizontal status süzgəc çipləri: *Hamısı*, *Gözləyir*, *Qəbul edildi*, *Hazırlanır*, *Hazırdır*, *İmtina*.
  - Hər sifariş kartında:
    - Sifariş nömrəsi (#BG-2026-XXXX) qızılı vurğu ilə (`Gold500`)
    - Mövzu, təhsil pilləsi və təhvil tarixi
    - Müştərinin adı və əlaqə telefonu
    - Vəziyyət nişanı (`StatusBadge`)
    - Əməliyyat düymələri: **"Bax"**, **"Çat"**, **"SMS / WhatsApp"** və **"İdarə et"**
- **SMS / WhatsApp Dialoq Pəncərəsi (`ContactCustomerBottomSheet`)**:
  - Müştərinin nömrəsi və adı göstərilir.
  - Hazır sürətli şablon düymələri:
    - *"Sifarişiniz qəbul edildi"*
    - *"Sifarişiniz üzrə əlavə məlumat tələb olunur"*
    - *"Sifarişiniz hazırdır"*
  - Redaktə edilə bilən mesaj mətni sahəsi.
  - İki aydın hərəkət düyməsi:
    1. **"SMS ilə Göndər"**: Telefonun daxili mesajlaşma tətbiqini pre-fill edilmiş mətnlə açır (`smsto:+994...`).
    2. **"WhatsApp ilə Göndər"**: WhatsApp tətbiqini müştərinin nömrəsi və mətni ilə açır (`https://api.whatsapp.com/send?phone=...`).
- **Müştərilər Bölməsi (`AdminCustomersContent`)**:
  - Hər müştəri üçün telefon nömrəsi yanında birbaşa SMS/WhatsApp düyməsi.

## 3. Key Product Decisions & Trade-Offs

- **Admin İcazəsi Mexanizmi**:
  - *Seçilmiş Yanaşma*: `elmarmmdov1@gmail.com` baş admin olaraq kod və təhlükəsizlik qaydaları səviyyəsində avtomatik admin hüququ alır. Eyni zamanda Firestore `users` kolleksiyasında `role == 'admin'` və ya `admins` siyahısındakı digər istifadəçilər də admin qəbul edilir.
  - *Səbəb*: Müştəri hesabı ilə admin hesabı eyni Firestore bazasında işləyir. Əvvəlki kodda Firebase Auth Custom Claim gözlənilirdi, lakin client-side auth sistemində claims olmadığı üçün admin hesabı adi `customer` kimi qəbul olunurdu və Firestore qaydaları bütün sifarişlərin oxunmasını bloklayırdı.
- **SMS və Mesajlaşma İnteqrasiyası**:
  - *Seçilmiş Yanaşma*: Android Native Intent (`Intent.ACTION_SENDTO` və WhatsApp universal link).
  - *Səbəb*: Əlavə ödənişli xarici SMS gateway (Twilio və s.) API açarı və ya abunəlik tələb etmədən birbaşa adminin öz cihazından rəsmi və ödənişsiz işləyir.

## 4. Technical Architecture & Data Strategy

### Arxitektura və Məlumat Axını Diaqramı

```
┌─────────────────────────────────────────────────────────┐
│                    Firestore Backend                    │
│  ┌──────────────────────┐     ┌──────────────────────┐  │
│  │   /orders/{orderId}  │     │   /users/{userId}    │  │
│  │  - userId            │     │  - email             │  │
│  │  - customerName      │     │  - role ("admin" /   │  │
│  │  - customerPhone     │     │         "customer")  │  │
│  │  - status            │     │  - phone             │  │
│  └──────────┬───────────┘     └──────────┬───────────┘  │
└─────────────┼────────────────────────────┼──────────────┘
              │ Real-time Snapshot         │ Profile & Role
              ▼                            ▼
┌─────────────────────────────────────────────────────────┐
│              Repository Layer (Kotlin Flow)             │
│   BestGroupRepository -> FirestoreOrderService          │
│   - if (isStaff) query: collection("orders")            │
│   - isStaff = email == "elmarmmdov1@gmail.com" || ...   │
└─────────────────────────────┬───────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────┐
│                   MainViewModel State                   │
│   - orders: StateFlow<List<Order>>                      │
│   - adminCustomers: StateFlow<List<User>>               │
│   - adminUpdateOrderStatus / adminUpdatePriority        │
└─────────────────────────────┬───────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────┐
│                  Admin UI Components                    │
│  ┌─────────────────────────┐   ┌─────────────────────┐  │
│  │ AdminDashboardScreen    │   │ ContactCustomerSheet│  │
│  │ - Orders Live List      │──▶│ - Intent SMS        │  │
│  │ - Filter Chips          │   │ - WhatsApp Action   │  │
│  └─────────────────────────┘   └─────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Həyata Keçiriləcək Addımlar:
1. **Firestore Qaydaları (`firestore.rules`)**:
   - `isStaff()` və `isAdmin()` funksiyalarına `request.auth.token.email == 'elmarmmdov1@gmail.com'` və Firestore `users` kolleksiyasındakı `role in ['admin', 'manager', 'operator']` yoxlamasının əlavə edilməsi.
   - `orders` kolleksiyasında admin üçün qeyd-şərtsiz oxuma/yazma icazəsinin verilməsi.
   - Dəyişikliyin `deploy_firebase` ilə serverə göndərilməsi.
2. **Auth & Repository Qatı (`FirebaseAuthService.kt`, `BestGroupRepository.kt`)**:
   - `elmarmmdov1@gmail.com` daxil olduqda avtomatik `UserRole.ADMIN` rolunun verilməsi və profil sənədində `role: "admin"` qeyd olunması.
   - Admin üçün `orderService.getOrdersFlow(userId, isStaff = true)` ilə bütün sifarişlərin real vaxt rejimində dinlənilməsinin təmin edilməsi.
   - Sifariş yaradılarkən sifariş obyektinə `customerName` və `customerPhone` sahələrinin yazılması.
3. **SMS & WhatsApp İnteqrasiyası (`AdminDashboardScreen.kt`, `OrderDetailScreen.kt`)**:
   - `ContactCustomerBottomSheet` komponentinin yaradılması (nömrə, hazır şablonlar, SMS və WhatsApp düymələri).
   - Android Intent köməkçisi ilə cihazın SMS tətbiqini (`smsto:`) və ya WhatsApp-ı açmaq.
4. **Yoxlama və Test**:
   - `compile_applet` ilə tətbiqin xətasız qurulmasının təsdiqlənməsi.
