# Sifarişlərin Canlı İzlənməsi, Oxunmuş Bildirişlər və Temanın Qalıcı Saxlanması Planı

Bu təkmilləşdirmə müştəri sifarişlərinin admin panelində canlı yenilənən tam siyahı və status süzgəcləri ilə ən son yaradılanlar ən üstdə olmaqla sıralanmasını, bildirişlər bölməsində oxunmuş mesajların oxunmamış kimi qalmasının qarşısının alınaraq solğun/oxunmuş rejimdə saxlanmasını və həm admin, həm də müştəri üçün seçilmiş görünüş temasının (işıqlı / tünd) tətbiqdən çıxıb daxil olduqda sıfırlanmadan qalıcı saxlanmasını təmin edir.

## User Review & Critical Decisions

> [!IMPORTANT]
> Təsdiqlənmiş qərarlar:
> - **Sifarişlərin Sıralanması**: Ən son daxil olan və yaradılan sifarişlər ən üstdə (azalan xronoloji ardıcıllıqla) əks olunacaq, status çipləri üfüqi sürüşdürmə və say göstəricisi ilə təqdim ediləcək.
> - **Oxunmuş Bildirişlərin Vəziyyəti**: Bildirişə toxunduqda və ya "Hamısını oxunmuş et" seçildikdə bildirişin oxunma statusu dərhal yenilənəcək, oxunmuş bildirişlər daha solğun rəngdə və qızılı oxunmamış nöqtəsi olmadan göstəriləcək; oxunmuş bildiriş heç bir halda yenidən oxunmamış kimi təqdim olunmayacaq.
> - **Görünüş Temasının Qalıcılığı**: Müştəri və ya admin profilindən seçilən tema rejimi (`LIGHT`, `DARK` və ya `SYSTEM`) cihazın `SharedPreferences` yaddaşına yazılacaq və tətbiq yenidən başladıqda avtomatik bərpa ediləcək. Həmçinin admin profilinə də xüsusi tema seçimi bloku əlavə ediləcək.

## 1. Overview & Core Concept

- **Nə edir**:
  - Admin paneldəki "Sifarişlər" bölməsində real vaxt rejimində gələn bütün sifarişlər ən son yaradılma vaxtına görə sıralanır və status süzgəcləri (`Hamısı`, `Gözləmədə`, `İcrada`, `Baxışda`, `Hazırdır`, `Ləğv edildi`) vasitəsilə asanlıqla filtirlənir.
  - Bildirişlərdə (müştəri və admin) oxunmuş bildirişlərin ID-ləri yadda saxlanılır, fərdi klik və qlobal "Hamısını oxunmuş et" əməliyyatları ilə status dərhal əks etdirilir və oxunmuş elementlər vizual olaraq oxunmamışlardan fərqləndirilir.
  - Tətbiqin görünüş rejimi (`ThemeMode.LIGHT` və ya `ThemeMode.DARK`) lokal yaddaşda saxlanılır; istifadəçi proqramı bağlayıb açsa belə seçdiyi tünd və ya işıqlı tema dəyişməz qalır.
- **Hədəf İstifadəçilər**: BestGroup.az müştəriləri və sistem idarəçiləri (`elmarmmdov1@gmail.com` və digər adminlər).
- **Əsas Dəyər**: Rahat sifariş idarəetməsi, təmiz və dəqiq bildiriş qeydiyyatı, fərdiləşdirilmiş və daimi vizual rahatlıq.

## 2. User Experience & Visual Design

- **Admin Sifarişlərin Siyahısı & Süzgəclər (`AdminOrdersContent`)**:
  - **Sürüşdürülə bilən status süzgəcləri**: `Hamısı (N)`, `Gözləyir (N)`, `İcrada (N)`, `Hazırdır (N)` və s. M3 `FilterChip` dizaynında aktiv/passiv vəziyyətlər.
  - **Avtomatik Sıralama**: Ən yeni sifarişlər dərhal siyahının başında yerləşir.
  - **Kart Dizaynı**: Müştərinin adı, telefon nömrəsi, fənn/mövzu, son tarix və dərhal əlaqə ("SMS / WA", "Bax", "İdarə") düymələri.
- **Bildirişlər Ekranı & Admin Bildiriş Mərkəzi**:
  - Oxunmamış bildirişlər: Tam parlaq kart, sol zolaqda və ya sağ yuxarı küncdə qızılı indikator nöqtəsi (`Gold500`).
  - Oxunmuş bildirişlər: Təbii solğunlaşdırma (`alpha = 0.65f`), normal şrift çəkisi, oxunmamış indikatorun gizlədilməsi.
  - Bildirişə toxunduqda: Sifarişə/çata keçidlə bərabər dərhal həmin bildiriş oxunmuş kimi qeyd edilir və yaddaşda saxlanılır.
- **Qalıcı Tema Dəstəyi**:
  - Müştəri üçün: Profil ekranında mövcud olan tema seçimi birbaşa yaddaşa qeyd olunur.
  - Admin üçün: Admin panelinin "Profil" tabında M3 kart daxilində "Görünüş Teması" seçimi (Açıq / Tünd / Sistem) təqdim olunur.
  - Tətbiq açılarkən sıfırlanma (flicker) olmadan sonuncu yadda saxlanmış tema aktivləşir.

## 3. Key Product Decisions & Trade-Offs

- **Lokal Tema Qalıcılığı (`SharedPreferences` & `StateFlow`)**:
  - *Seçilmiş Yanaşma*: `AppPreferencesManager` sinfi yaradılır və Android `SharedPreferences` vasitəsilə tema rejimi və oxunmuş bildiriş ID-ləri sinxron saxlanılır.
  - *Səbəb*: Server sorğusu tələb etmir, cihazda dərhal açılır və internet bağlantısı olmadıqda belə temanı və oxunma statusunu qoruyur.
- **Oxunmuş Bildirişlərin Birləşdirilməsi**:
  - *Seçilmiş Yanaşma*: Bildirişlər axını (`notifications`) yadda saxlanılan oxunmuş ID-lər çoxluğu (`readNotificationIds`) ilə birləşdirilir (`isRead = isRead || readIds.contains(id)`).
  - *Səbəb*: Yeni dinamik bildirişlər (məsələn yeni sifariş və ya status dəyişikliyi) daxil olanda əvvəl oxunmuşlar oxunmamış vəziyyətinə qayıtmır.

## 4. Technical Architecture & Data Strategy

### Arxitektura və Məlumat Axını Diaqramı

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AppPreferencesManager                           │
│  - saveThemeMode(mode: ThemeMode) / getSavedThemeMode(): ThemeMode     │
│  - markNotificationAsRead(id) / getReadNotificationIds(): Set<String>  │
│  - markAllNotificationsAsRead(ids: List<String>)                       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Persistent read/write
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        BestGroupRepository                             │
│  - _themeMode: MutableStateFlow<ThemeMode> (initial from Preferences)  │
│  - _readNotificationIds: MutableStateFlow<Set<String>>                 │
│  - orders: StateFlow<List<Order>> (sortedByDescending { createdAt })   │
│  - markNotificationAsRead(id: String)                                  │
│  - markAllNotificationsAsRead()                                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ StateFlow Observation
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
┌───────────────────────────────────────┐   ┌────────────────────────────┐
│          Admin Panel UI               │   │    Müştəri & Bildirişlər   │
│ - AdminOrdersContent:                 │   │ - NotificationsScreen:     │
│   * Live descending orders            │   │   * Dimmed read cards      │
│   * Horizontal scrollable chips       │   │   * Persistent read state  │
│   * Status filter with order counts   │   │ - ProfileScreen & Theme:   │
│ - AdminProfileContent:                │   │   * Persistent ThemeMode   │
│   * Theme switcher (Light/Dark/System)│   │   * Preserved on restart   │
└───────────────────────────────────────┘   └────────────────────────────┘
```

### Həyata Keçiriləcək Dəqiq Addımlar:
1. **Davamlı Yaddaş Qatı (`AppPreferencesManager.kt`)**:
   - `ThemeMode` və oxunmuş bildirişlərin ID siyahısını `SharedPreferences`-də saxlayan köməkçi sinif hazırlamaq.
2. **Repozitoriya və ViewModel İnteqrasiyası (`BestGroupRepository.kt`, `MainViewModel.kt`)**:
   - Tətbiq açılarkən yadda saxlanmış temanı və oxunmuş bildirişləri yükləmək.
   - `markNotificationAsRead(id: String)` funksiyasını əlavə etmək və oxunma vəziyyətini həm yaddaşa, həm də axına tətbiq etmək.
   - Sifarişlər axınında `sortedByDescending { it.createdAt }` sıralamasını zəmanətləndirmək.
3. **Admin Sifarişlər Siyahısının Təkmilləşdirilməsi (`AdminDashboardScreen.kt`)**:
   - Süzgəcləri üfüqi sürüşdürülə bilən (`horizontalScroll`) formaya salmaq və hər statusun qarşısında sifariş sayını göstərmək.
   - Admin bildiriş siyahısında oxunmuş elementləri daha solğun və oxunmamışları parlaq nöqtə ilə təqdim etmək, toxunulduqda oxunmuş kimi qeyd etmək.
   - Admin profilinə "Görünüş Teması" seçici blokunu əlavə etmək.
4. **Müştəri Bildirişlər və Tema Ekranı (`NotificationsScreen.kt`, `ProfileScreen.kt`, `MainActivity.kt`)**:
   - Bildiriş kliklərində dərhal oxunmuş statusunu tətbiq etmək.
   - Tətbiq yenidən açıldıqda seçilmiş temanın dəyişməz qalmasını yoxlamaq və təmin etmək.
5. **Kompilyasiya və Yoxlama**:
   - `compile_applet` ilə tətbiqin qüsursuz qurulmasını təsdiq etmək.
