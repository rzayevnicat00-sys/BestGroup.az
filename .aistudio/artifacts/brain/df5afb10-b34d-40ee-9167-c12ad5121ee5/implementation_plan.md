# BestGroup.az: Admin & Müştəri İnterfeysi, Firestore Real-Time Sifarişlər və Təhlükəsizlik Sistemi

BestGroup.az tətbiqində Admin (`elmarmmdov1@gmail.com`) və Müştəri rollarının həm interfeys, həm də Firebase təhlükəsizlik səviyyəsində tam ayrılması, müştəri sifarişlərinin real vaxt rejimində Firestore vasitəsilə admin panelinə dərhal çatdırılması, admin çatında müştəri müraciətlərinin canlı göstərilməsi və hərtərəfli təhlükəsizlik yoxlanışının təmin edilməsi planı.

## User Review & Critical Decisions

> [!IMPORTANT]
> İstifadəçi tərəfindən Phase 1 sualları vasitəsilə təsdiqlənmiş qərarlar və sistem qaydaları:
> - **Sifariş xətası bildirişi**: Sifariş yaradılarkən Firestore xətası baş verərsə, müştəriyə qırmızı Snackbar xəta bildirişi göstəriləcək və təkrar cəhd imkanı veriləcək (uğursuz əməliyyat heç vaxt uğurlu kimi təqdim edilməyəcək).
> - **Admin çat interfeysi**: Admin panelində müştəri seçildikdə dialoq admin paneli daxilində tam ekran canlı söhbət ekranı kimi açılacaq və admin dərhal müştəriyə cavab yaza biləcək.
> - **Admin hesabı və hüquqlar**: Yalnız `elmarmmdov1@gmail.com` super admin səlahiyyətinə malikdir. Müştəri üçün admin paneli qətiyyən görünməyəcək, admin profilində isə müştəriyə aid "Yeni sifariş" düyməsi və "Hazırda aktiv sifarişiniz yoxdur" kimi müştəri blokları tam gizlədiləcək.

---

## 1. Overview & Core Concept

### Nə təmin olunacaq?
1. **İstifadəçi Rollarının Qəti Ayrılması**:
   - `elmarmmdov1@gmail.com` ünvanı üçün Admin hüquqları server tərəfli (Firestore Rules, Custom Claims və Firestore `users/{uid}.role`) ilə yoxlanılır.
   - Digər bütün istifadəçilər ciddi şəkildə müştəri (`UserRole.CUSTOMER`) hesab edilir.
   - Admin profilində müştərilərə aid sifariş vermə funksiyaları (Ana səhifədəki və digər bölmələrdəki "Yeni sifariş" düymələri, "Hazırda aktiv sifarişiniz yoxdur" boşluq bloku) tam yığışdırılır.
   - Müştəri profilində Admin İdarəetmə Paneli heç bir halda göstərilmir.
2. **Sifarişlərin Real-Time Firestore İnteqrasiyası**:
   - Müştəri sifariş verdikdə sifariş dərhal Firestore `orders` kolleksiyasına yazılır (`orderId`, `userId`, `customerName`, `serviceType`, `topic`, `deadline`, `status`, `createdAt`, `estimatedPriceAzn`, `priority`, və s.).
   - Sifariş yazılarkən xəta baş verərsə dərhal tutulur və qırmızı bildirişlə istifadəçiyə bildirilir.
   - Admin paneli Firestore `orders` kolleksiyasını `addSnapshotListener` vasitəsilə dinləyir, tətbiq bağlanıb-açılmadan sifarişlər dərhal canlı olaraq ekranda görünür.
   - Admin sifarişin statusunu dəyişdikdə (məsələn, "Qəbul edildi", "Hazırlanır", "Tamamlandı") müştəri ekranında dərhal avtomatik yenilənir.
3. **Admin Canlı Çat Sistemi**:
   - Admin müştərilərin Müştəri Xidmətlərinə yazdığı bütün sorğuları və sifariş söhbətlərini Firestore-dan yükləyərək birbaşa siyahı şəklində görür.
   - "Fəal söhbət yoxdur, Sifarişlər bölməsindən kuratorla əlaqəli söhbəti aça bilərsiniz" kimi müştəriyə aid çaşdırıcı mesajlar admində göstərilmir.
   - Admin siyahıdan istənilən müştəri müraciətini seçərək admin paneli daxilində canlı cavab yazır, mesajların çatdırılma və oxunma vəziyyətləri real vaxtda sinxronlaşır.
4. **Firebase Təhlükəsizlik və İcazə Qaydaları (`firestore.rules`)**:
   - Müştərilər yalnız öz `userId`-lərinə aid sifarişləri və söhbətləri oxuya və yarada bilər; başqalarının sifariş və fayllarına çıxış bloklanır.
   - Admin hüquqları server səviyyəsində yoxlanılır.

---

## 2. User Experience & Visual Design

### A. Admin İstifadəçi Axını
- **Giriş**: `elmarmmdov1@gmail.com` daxil olduqda sistem onu avtomatik olaraq Admin kimi tanıyır.
- **Ana Səhifə (Home)**: Müştəriyə aid "Yeni sifariş ver" hero kartı və "Hazırda aktiv sifarişiniz yoxdur" yazısı yerinə, "Admin İdarəetmə Paneli" qısayolu və sistemin ümumi canlı vəziyyətini göstərən statistik kart nümayiş olunur.
- **Admin Dashboard**:
  - *Sifarişlər bölməsi*: Bütün müştərilərdən gələn canlı sifarişlər (filtr: Hamısı, Gözləmədə, Qəbul edildi, Hazırlanır, Hazırdır). Status dəyişmə və müştəri ilə dərhal əlaqə (zəng / WhatsApp / Çat).
  - *Müştəri Xidmətləri (Çat) bölməsi*: Bütün müştəri müraciətlərinin siyahısı (müştərinin adı, nömrəsi, son mesaj vaxtı və oxunmamış bildiriş sayı). Seçildikdə birbaşa daxilində canlı cavablaşma.
  - *Müştərilər bölməsi*: Qeydiyyatdan keçmiş bütün istifadəçilər və onların sifariş sayları.
  - *Statistikalar və Bildirişlər*: Ümumi dövriyyə, aktiv sifarişlər və sistem bildirişləri.

### B. Müştəri İstifadəçi Axını
- **Ana Səhifə (Home)**: Sifariş kataloqu, 8 addımlı rahat sifariş sistemi, müştərinin özünə aid aktiv sifarişləri (əgər varsa). Heç bir admin keçidi görünmür.
- **Sifariş Yaratma**: Bütün tələb olunan sahələr (`orderId`, `userId`, `topic`, `deadline`, və s.) daxil edilərək təsdiqlənir. Uğursuz olduqda qırmızı bildiriş və təkrar cəhd düyməsi göstərilir.
- **Çat**: Müştəri xidmətlərinə birbaşa canlı sual ünvanlama imkanı.

### C. Vizual Dizayn & Dil Dəstəyi
- Premium Navy (`#0A192F`, `#0F2442`) və Akademik Qızılı (`#D4AF37`, `#FFD700`) rəng palitrası qorunur.
- Bütün mətnlər və etiketlər Azərbaycan, İngilis və Rus dillərində tam lokallaşdırılır (`LocaleAz`, `LocaleEn`, `LocaleRu`).
- Qaranlıq və İşıqlı rejim seçimi (`ThemeMode`) həm admin, həm də müştəri üçün yadda saxlanılır və tətbiq bağlanıb-açıldıqda dəyişmir.

---

## 3. Key Technical Decisions & Trade-Offs

### Qərar 1: Firestore Real-Time Dinləyicilərinin Təşkili
- **Seçilən yanaşma**: `FirestoreOrderService` və `FirestoreChatService` daxilində `callbackFlow` və `addSnapshotListener` mexanizmindən istifadə olunur.
- **Səbəb**: Birdəfəlik `get()` sorğusu sifarişlərin yalnız səhifə yenilənərkən görünməsinə səbəb olurdu. `SnapshotListener` isə müştəri sifariş göndərən anda Firestore tərəfindən bildiriş alaraq Admin ekranında sifarişin 1 saniyədən az müddətdə avtomatik peyda olmasını təmin edir.

### Qərar 2: Admin Rollarının Server və Müştəri Tərəfli Təsdiqi
- **Seçilən yanaşma**:
  1. Server tərəfində: `firestore.rules` faylında `isSuperAdmin()` qaydası `elmarmmdov1@gmail.com` üçün icazə verir, həmçinin `users/{uid}.role == 'admin'` və Custom Claims yoxlayır.
  2. Tətbiq daxilində: `FirebaseAuthService.isSuperAdminEmail(email)` və Firestore `users` kolleksiyasındakı profil sinxronlaşdırılır.
  3. `elmarmmdov1@gmail.com` xaricində heç bir istifadəçiyə avtomatik admin rolu verilmir.

### Qərar 3: Xətaların Şəffaf İdarə Edilməsi
- **Seçilən yanaşma**: `createOrder` metodunda Firestore yazılışı uğursuz olduqda yalançı lokal sifariş yaradılmır, əməliyyat xəta fırladır və UI-da qırmızı Snackbar göstərilir. Yalnız Firestore sənədi uğurla yazıldıqdan sonra sifariş təsdiqlənmiş sayılır.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        BestGroup.az Android App                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
           ┌────────────────────────┴────────────────────────┐
           ▼                                                 ▼
┌──────────────────────────────┐              ┌──────────────────────────────┐
│       CUSTOMER FLOW          │              │          ADMIN FLOW          │
│ • Home (Sifariş yarat, FAQ)  │              │ • Admin Dashboard (Sifarişlər)│
│ • Order Wizard (8 addım)     │              │ • Real-time Customer Orders  │
│ • Customer Support Chat      │              │ • Real-time Support Chats    │
│ • Orders Screen (özünə aid)  │              │ • Customer Management & Stats│
└──────────────┬───────────────┘              └──────────────┬───────────────┘
               │                                             │
               │ createOrder()                               │ SnapshotListener (all)
               │ (only own uid)                              │ updateStatus()
               ▼                                             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    Firebase Firestore Services                         │
│ • FirestoreOrderService: callbackFlow(ordersCollection)               │
│ • FirestoreChatService: callbackFlow(conversationsCollection)          │
│ • FirebaseAuthService: Google / Email auth + Admin Claims check        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               Firestore Zero-Trust Security Rules                      │
│ • /orders/{orderId}:                                                   │
│   - read: isStaff() || (auth.uid == resource.data.userId)              │
│   - create: auth.uid == request.resource.data.userId                  │
│   - update: isStaff() || (auth.uid == resource.data.userId & locked)   │
│ • /conversations/{convId}:                                             │
│   - read/write: isStaff() || (auth.uid in resource.data.participants)  │
│ • /users/{userId}:                                                     │
│   - read: isStaff() || auth.uid == userId                              │
│   - role upgrade: strictly forbidden unless server/admin               │
└────────────────────────────────────────────────────────────────────────┘
```

### Dəyişiklik Ediləcək Əsas Komponentlər
1. **`firestore.rules`**: Admin və müştəri hüquqlarının, kolleksiya sorğularının və real-time snapshot dinləyicilərinin icazələrinin server səviyyəsində dəqiqləşdirilməsi.
2. **`FirestoreOrderService.kt`**:
   - `createOrder`: Firestore-a bütün zəruri sahələrin (`orderId`, `userId`, `customerName`, `serviceType`, `topic`, `deadline`, `status`, `createdAt`, `files`, və s.) yazılmasının və xəta idarəsinin təmin edilməsi.
   - `getOrdersFlow`: Admin üçün bütün sifarişlərin, müştəri üçün isə yalnız öz sifarişlərinin real-time `snapshots` ilə axına verilməsi.
   - `updateOrderStatus`: Status dəyişdikdə tarixçə və yenilənmə vaxtının Firestore-da yenilənməsi.
3. **`FirestoreChatService.kt`**:
   - Admin üçün bütün müştəri müraciətlərinin real-time yüklənməsi.
   - `orderId` və `userId` əlaqələndirilməsinin düzgün təmin edilməsi.
   - Mesaj oxunma və göndərmə statuslarının sinxronlaşdırılması.
4. **`BestGroupRepository.kt` & `MainViewModel.kt`**:
   - Sifariş yaradılarkən Firestore xətasının UI-a ötürülməsi (Snackbar üçün xəta axını).
   - Admin panelində sifarişlərin dərhal yenilənməsi üçün `observeFirestoreData` mexanizminin gücləndirilməsi.
5. **`HomeScreen.kt` & `OrdersScreen.kt` & `ChatScreen.kt` & `AdminDashboardScreen.kt`**:
   - Admin profilində "Yeni sifariş" və "Hazırda aktiv sifarişiniz yoxdur" hissələrinin təmizlənməsi.
   - Müştəri profilində Admin panelinə aid elementlərin gizlədilməsi.
   - Admin Support bölməsində müştəri ilə birbaşa canlı söhbət ekranının işlək təminatı.

---

## 5. Deployment & Testing Strategy

- `compile_applet` vasitəsilə layihənin sintaksis və Gradle quruluşunun yoxlanması.
- `deploy_firebase` vasitəsilə yenilənmiş `firestore.rules` təhlükəsizlik qaydalarının backend-ə yayımlanması.
- Real vaxt rejimində sifariş və çat ssenarilərinin yoxlanışı və ətraflı yekun hesabatın təqdim edilməsi.
