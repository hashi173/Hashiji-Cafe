package com.coffeeshop.config;

import com.coffeeshop.entity.JobPosting;
import com.coffeeshop.entity.Role;
import com.coffeeshop.repository.JobPostingRepository;
import com.coffeeshop.repository.CategoryRepository;
import com.coffeeshop.repository.ProductRepository;
import com.coffeeshop.repository.ProductSizeRepository;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.OrderDetailRepository;
import com.coffeeshop.repository.ExpenseRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.repository.WorkShiftRepository;
import com.coffeeshop.repository.IngredientRepository;
import com.coffeeshop.entity.Ingredient;
import com.coffeeshop.repository.ProductRecipeRepository;
import com.coffeeshop.entity.ProductRecipe;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

        private final JobPostingRepository jobPostingRepository;
        private final CategoryRepository categoryRepository;
        private final ProductRepository productRepository;
        private final ProductSizeRepository productSizeRepository;
        private final OrderRepository orderRepository;
        private final OrderDetailRepository orderDetailRepository;
        private final ExpenseRepository expenseRepository;
        private final UserRepository userRepository;
        private final WorkShiftRepository workShiftRepository;
        private final IngredientRepository ingredientRepository;
        private final ProductRecipeRepository productRecipeRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
                // 1. Master Cleanup
                cleanupData();

                // 2. Seed basics
                seedJobs();
                seedProducts();
                seedIngredients();
                seedRecipes();

                // 3. Seed Users
                seedUsers();

                // 4. Seed History (Orders depend on Users & Products)
                seedHistory();

                // 5. Seed Shifts (Depend on Users)
                seedShifts();

                // 6. Seed Active Data (Today's Open Shifts & Active Orders)
                seedActiveData();
        }

        private void cleanupData() {
                try {
                        orderDetailRepository.deleteAll();
                        orderRepository.deleteAll();
                        expenseRepository.deleteAll();
                        workShiftRepository.deleteAll();
                        userRepository.deleteAll();
                } catch (Exception e) {
                        System.out.println("Warning during cleanup: " + e.getMessage());
                }
        }

        private void seedUsers() {
                // Create Admin
                if (userRepository.findByUsername("admin").isEmpty()) {
                        com.coffeeshop.entity.User admin = new com.coffeeshop.entity.User();
                        admin.setUsername("admin");
                        admin.setPassword(passwordEncoder.encode("123456"));
                        admin.setFullName("System Administrator");
                        admin.setRole(Role.ADMIN);
                        admin.setUserCode("ADM01");
                        admin.setActive(true);
                        userRepository.save(admin);
                }

                // Create 5 Staff Members
                createStaff("manager", "Nguyen Van An", "manager@coffee.com", "0901234567", Role.ADMIN, 50000.0,
                                "S001");
                createStaff("barista1", "Tran Thi Binh", "barista1@coffee.com", "0901112222", Role.STAFF, 35000.0,
                                "S002");
                createStaff("barista2", "Le Van Cuong", "barista2@coffee.com", "0903334444", Role.STAFF, 35000.0,
                                "S003");
                createStaff("server1", "Pham Thi Dung", "server1@coffee.com", "0905556666", Role.STAFF, 25000.0,
                                "S004");
                createStaff("server2", "Hoang Van Em", "server2@coffee.com", "0907778888", Role.STAFF, 25000.0, "S005");
        }

        private void createStaff(String username, String fullName, String email, String phone, Role role,
                        Double salary, String userCode) {
                if (userRepository.findByUsername(username).isPresent())
                        return;

                com.coffeeshop.entity.User u = new com.coffeeshop.entity.User();
                u.setUsername(username);
                u.setPassword(passwordEncoder.encode("123456"));
                u.setFullName(fullName);
                u.setEmail(email);
                u.setPhone(phone);
                u.setRole(role);
                u.setHourlyRate(salary);
                u.setUserCode(userCode);
                u.setActive(true);
                userRepository.save(u);
        }

        private void seedHistory() {
                com.coffeeshop.entity.User admin = userRepository.findByUsername("admin").orElse(null);
                if (admin == null)
                        return;

                java.util.List<com.coffeeshop.entity.Product> products = productRepository.findAll();
                if (products.isEmpty())
                        return;

                java.util.Random rand = new java.util.Random();
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                // Get all users to distribute orders
                java.util.List<com.coffeeshop.entity.User> allUsers = userRepository.findAll();

                // Generate last 10 months (0..9)
                for (int i = 9; i >= 0; i--) {
                        java.time.LocalDateTime currentMonth = now.minusMonths(i);
                        int year = currentMonth.getYear();
                        int month = currentMonth.getMonthValue();

                        // 250-450 orders per month for profitable data
                        int ordersCount = 250 + rand.nextInt(200);

                        // If it's the current month, limit orders to days passed so far
                        int maxDay = currentMonth.toLocalDate().lengthOfMonth();
                        if (currentMonth.getMonthValue() == now.getMonthValue()
                                        && currentMonth.getYear() == now.getYear()) {
                                maxDay = now.getDayOfMonth();
                        }

                        for (int j = 0; j < ordersCount; j++) {
                                com.coffeeshop.entity.Order order = new com.coffeeshop.entity.Order();

                                // Assign to random user (or admin)
                                if (!allUsers.isEmpty()) {
                                        order.setUser(allUsers.get(rand.nextInt(allUsers.size())));
                                } else {
                                        order.setUser(admin);
                                }

                                order.setCustomerName("Customer " + (j + 1));
                                order.setOrderType("POS Order");
                                order.setStatus(com.coffeeshop.entity.OrderStatus.COMPLETED);

                                // Random date within the month
                                if (maxDay < 1)
                                        maxDay = 1;
                                int day = 1 + rand.nextInt(maxDay);

                                order.setCreatedAt(currentMonth.withDayOfMonth(day)
                                                .withHour(8 + rand.nextInt(12))
                                                .withMinute(rand.nextInt(60)));

                                // Add items
                                double total = 0;
                                int itemsCount = 1 + rand.nextInt(3);
                                java.util.List<com.coffeeshop.entity.OrderDetail> details = new java.util.ArrayList<>();

                                for (int k = 0; k < itemsCount; k++) {
                                        com.coffeeshop.entity.Product p = products.get(rand.nextInt(products.size()));
                                        com.coffeeshop.entity.OrderDetail d = new com.coffeeshop.entity.OrderDetail();
                                        d.setOrder(order);
                                        d.setProduct(p);
                                        d.setProductName(p.getName());
                                        d.setSizeSelected("Standard");
                                        d.setQuantity(1 + rand.nextInt(2));

                                        // Simple price logic for seeding
                                        double price = 40000.0 + rand.nextInt(30) * 1000;
                                        d.setPriceAtPurchase(price);

                                        total += price * d.getQuantity();
                                        details.add(d);
                                }

                                order.setTotalAmount(total);
                                order.setTrackingCode(
                                                java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());

                                com.coffeeshop.entity.Order saved = orderRepository.save(order);
                                for (com.coffeeshop.entity.OrderDetail d : details) {
                                        d.setOrder(saved);
                                        orderDetailRepository.save(d);
                                }
                        }

                        // Generate Expenses for this month
                        com.coffeeshop.entity.Expense utilityExpense = new com.coffeeshop.entity.Expense();
                        utilityExpense.setDescription("Utilities " + month + "/" + year);
                        utilityExpense.setAmount(1_200_000.0 + rand.nextInt(500_000));
                        utilityExpense.setCategory("Utilities");
                        utilityExpense.setExpenseDate(currentMonth.toLocalDate().withDayOfMonth(5));
                        expenseRepository.save(utilityExpense);

                        com.coffeeshop.entity.Expense ingredientExpense = new com.coffeeshop.entity.Expense();
                        ingredientExpense.setDescription("Ingredients Supply " + month + "/" + year);
                        ingredientExpense.setAmount(3_000_000.0 + rand.nextInt(2_000_000));
                        ingredientExpense.setCategory("Ingredients");
                        ingredientExpense.setExpenseDate(currentMonth.toLocalDate().withDayOfMonth(10));
                        expenseRepository.save(ingredientExpense);

                        // Rent
                        com.coffeeshop.entity.Expense rent = new com.coffeeshop.entity.Expense();
                        rent.setDescription("Shop Rent " + month + "/" + year);
                        rent.setAmount(5_000_000.0);
                        rent.setCategory("Rent");
                        rent.setExpenseDate(currentMonth.toLocalDate().withDayOfMonth(1));
                        expenseRepository.save(rent);

                        System.out.println("✓ Seeded " + ordersCount + " orders for " + month + "/" + year);
                }
                System.out.println("========================================");
                System.out.println("History data seeded (Last 10 Months)");
                System.out.println("========================================");
        }

        private void seedShifts() {
                // Generate shifts for the last 14 days for all staff
                java.time.LocalDate today = java.time.LocalDate.now();
                java.util.List<com.coffeeshop.entity.User> staff = userRepository.findAll();

                for (int i = 1; i < 14; i++) {
                        java.time.LocalDate date = today.minusDays(i);

                        for (com.coffeeshop.entity.User u : staff) {
                                if ("admin".equals(u.getUsername()))
                                        continue;

                                // Morning Shift 07:00 - 15:00
                                if (date.getDayOfMonth() % 2 == 0) {
                                        createShift(u, date.atTime(7, 0), date.atTime(15, 0), 2_000_000.0);
                                } else {
                                        // Afternoon Shift 14:00 - 22:00
                                        createShift(u, date.atTime(14, 0), date.atTime(22, 0), 2_500_000.0);
                                }
                        }
                }
                System.out.println("Shifts seeded successfully.");
        }

        private void createShift(com.coffeeshop.entity.User user, java.time.LocalDateTime start,
                        java.time.LocalDateTime end,
                        Double revenue) {
                com.coffeeshop.entity.WorkShift s = new com.coffeeshop.entity.WorkShift();
                s.setUser(user);
                s.setStartTime(start);
                s.setEndTime(end);
                s.setStartCash(1_000_000.0);
                s.setEndCash(1_000_000.0 + (revenue != null ? revenue : 0.0));
                s.setTotalRevenue(revenue != null ? revenue : 0.0);
                s.setStatus(end == null ? com.coffeeshop.entity.ShiftStatus.OPEN
                                : com.coffeeshop.entity.ShiftStatus.CLOSED);
                workShiftRepository.save(s);
        }

        private void seedActiveData() {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.util.List<com.coffeeshop.entity.Product> products = productRepository.findAll();
                java.util.Random rand = new java.util.Random();

                // 1. Create Active (OPEN) Shifts for today
                // Manager and Barista1 are working right now
                com.coffeeshop.entity.User manager = userRepository.findByUsername("manager").orElse(null);
                com.coffeeshop.entity.User barista1 = userRepository.findByUsername("barista1").orElse(null);

                if (manager != null) {
                        // Started 4 hours ago, still open (endTime null, revenue null)
                        createShift(manager, now.minusHours(4), null, null);
                }
                if (barista1 != null) {
                        // Started 2 hours ago
                        createShift(barista1, now.minusHours(2), null, null);
                }
                System.out.println("Active Shifts seeded.");

                // 2. Create Active Orders (PENDING / PROCESSING) for today
                // Create 5 active orders
                for (int i = 0; i < 5; i++) {
                        com.coffeeshop.entity.Order order = new com.coffeeshop.entity.Order();

                        // Assign to barista1 if available, else admin
                        order.setUser(barista1 != null ? barista1
                                        : userRepository.findByUsername("admin").orElse(null));
                        order.setCustomerName("Active Customer " + (i + 1));
                        order.setOrderType("POS Order");

                        // Mix statuses
                        if (i % 2 == 0) {
                                order.setStatus(com.coffeeshop.entity.OrderStatus.PENDING);
                        } else {
                                order.setStatus(com.coffeeshop.entity.OrderStatus.CONFIRMED); // CONFIRMED/PROCESSING
                        }

                        // Created recently (10-30 mins ago)
                        order.setCreatedAt(now.minusMinutes(10 + (i * 15)));

                        // Add items
                        double total = 0;
                        int itemsCount = 1 + rand.nextInt(2);
                        java.util.List<com.coffeeshop.entity.OrderDetail> details = new java.util.ArrayList<>();

                        for (int k = 0; k < itemsCount; k++) {
                                com.coffeeshop.entity.Product p = products.get(rand.nextInt(products.size()));
                                com.coffeeshop.entity.OrderDetail d = new com.coffeeshop.entity.OrderDetail();
                                d.setOrder(order);
                                d.setProduct(p);
                                d.setProductName(p.getName());
                                d.setSizeSelected("Standard");
                                d.setQuantity(1);
                                double price = 45000.0;
                                d.setPriceAtPurchase(price);
                                total += price * d.getQuantity();
                                details.add(d);
                        }
                        order.setTotalAmount(total);
                        order.setTrackingCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());

                        com.coffeeshop.entity.Order saved = orderRepository.save(order);
                        for (com.coffeeshop.entity.OrderDetail d : details) {
                                d.setOrder(saved);
                                orderDetailRepository.save(d);
                        }
                }
                System.out.println("Active Orders seeded.");
        }

        private void seedJobs() {
                if (jobPostingRepository.count() == 0) {
                        JobPosting job1 = new JobPosting();
                        job1.setTitle("Senior Barista");
                        job1.setLocation("District 1, HCMC");
                        job1.setType(com.coffeeshop.entity.JobType.FULL_TIME);
                        job1.setDescription(
                                        "We are looking for an experienced Barista to join our team. Must have 2 years of experience.");
                        job1.setRequirements("2+ years experience, Latte Art skills, Customer service oriented.");
                        job1.setActive(true);

                        JobPosting job2 = new JobPosting();
                        job2.setTitle("Store Manager");
                        job2.setLocation("District 3, HCMC");
                        job2.setType(com.coffeeshop.entity.JobType.FULL_TIME);
                        job2.setDescription("Manage daily operations, staff scheduling, and inventory.");
                        job2.setRequirements("Leadership experience, Management background.");
                        job2.setActive(true);

                        JobPosting job3 = new JobPosting();
                        job3.setTitle("Service Staff");
                        job3.setLocation("All Branches");
                        job3.setType(com.coffeeshop.entity.JobType.PART_TIME);
                        job3.setDescription("Join our vibrant team as service staff. Flexible hours for students.");
                        job3.setRequirements("Friendly, Energetic, Team player.");
                        job3.setActive(true);

                        jobPostingRepository.save(job1);
                        jobPostingRepository.save(job2);
                        jobPostingRepository.save(job3);

                        System.out.println("Job Postings seeded successfully.");
                }
        }

        private void seedProducts() {
                // Ensure categories exist
                com.coffeeshop.entity.Category coffee = createDetailsCategory("Coffee", "Premium beans from highlands");
                com.coffeeshop.entity.Category tea = createDetailsCategory("Tea", "Organic tea leaves");
                com.coffeeshop.entity.Category smoothie = createDetailsCategory("Smoothie", "Milk blended drinks");
                com.coffeeshop.entity.Category juice = createDetailsCategory("Juice", "Fresh pressed fruits");

                // --- Coffee Category ---
                createProduct("Cafe Latte", "Cà phê Latte",
                                "Creamy espresso with steamed milk and a butterfly pea flower infusion, topped with sea salt foam.",
                                "Espresso kem mịn với sữa nóng hoa đậu biếc, phủ bọt kem muối biển.",
                                "milk,sữa,cream,kem,espresso,coffee,cà phê,latte,sweet,ngọt,hot,nóng,creamy,béo",
                                "/images/products/CaffeLatte.png", coffee, 55000.0);

                createProduct("Espresso", "Espresso",
                                "Intense double-shot espresso with edible gold dust. Bold, dark, and aromatic.",
                                "Espresso đậm đà hai shot với bụi vàng ăn được. Đậm, đen và thơm nồng.",
                                "espresso,coffee,cà phê,strong,mạnh,bitter,đắng,black,đen,no milk,không sữa,bold,shot",
                                "/images/products/Espresso.png", coffee, 45000.0);

                // --- Tea Category ---
                createProduct("Peach Tea", "Trà Đào",
                                "Refreshing peach tea with fresh peach slices, mint leaves, and light sweetness. Served iced.",
                                "Trà đào thanh mát với lát đào tươi, lá bạc hà và vị ngọt nhẹ. Phục vụ đá.",
                                "tea,trà,peach,đào,fruit,trái cây,refreshing,mát,iced,đá,sweet,ngọt,no milk,không sữa",
                                "/images/products/PeachTea.png", tea, 55000.0);

                createProduct("Sakura Blossom Tea", "Trà Hoa Anh Đào",
                                "Delicate cherry blossom infused tea with subtle floral notes. Light and elegant.",
                                "Trà tinh tế hương hoa anh đào với hương hoa nhẹ nhàng. Thanh và tinh tế.",
                                "tea,trà,sakura,cherry blossom,hoa anh đào,floral,hoa,light,nhẹ,no milk,không sữa,elegant",
                                "/images/products/SakuraBlossomTea.png", tea, 58000.0);

                // --- Smoothie Category ---
                createProduct("Strawberry Smoothie", "Sinh tố Dâu",
                                "Thick strawberry smoothie blended with fresh milk, condensed milk, and real strawberries.",
                                "Sinh tố dâu đặc sánh xay với sữa tươi, sữa đặc và dâu tây thật.",
                                "smoothie,sinh tố,strawberry,dâu,milk,sữa,cream,kem,sweet,ngọt,cold,lạnh,fruit,trái cây,blended,xay,condensed milk,sữa đặc",
                                "/images/products/StrawberrySmoothie.png", smoothie, 60000.0);

                // --- Juice Category ---
                createProduct("Coconut Juice", "Nước Dừa",
                                "Fresh young coconut water with coconut jelly. Natural and hydrating with no dairy.",
                                "Nước dừa tươi non với thạch dừa. Tự nhiên và bổ dưỡng, không sữa.",
                                "coconut,dừa,juice,nước,fresh,tươi,no milk,không sữa,natural,tự nhiên,hydrating,cold,lạnh,healthy,no sugar,ít đường",
                                "/images/products/CoconutJuice.png", juice, 50000.0);

                System.out.println("Products seeded with AI tags.");
        }

        private com.coffeeshop.entity.Category createDetailsCategory(String name, String desc) {
                return categoryRepository.findByName(name)
                                .orElseGet(() -> {
                                        com.coffeeshop.entity.Category c = new com.coffeeshop.entity.Category();
                                        c.setName(name);
                                        c.setNameVi(name);
                                        c.setDescription(desc);
                                        return categoryRepository.save(c);
                                });
        }

        private void createProduct(String name, String nameVi, String desc, String descVi,
                        String tags, String url, com.coffeeshop.entity.Category cat, Double basePrice) {
                com.coffeeshop.entity.Product p = productRepository.findAll().stream()
                                .filter(product -> product.getName().equalsIgnoreCase(name))
                                .findFirst()
                                .orElse(new com.coffeeshop.entity.Product());

                p.setName(name);
                p.setNameVi(nameVi);
                p.setDescription(desc);
                p.setDescriptionVi(descVi);
                p.setTags(tags);
                p.setImage(url);
                p.setCategory(cat);
                p.setActive(true);
                com.coffeeshop.entity.Product saved = productRepository.save(p);

                if (productSizeRepository.findByProductId(saved.getId()).isEmpty()) {
                        com.coffeeshop.entity.ProductSize s = new com.coffeeshop.entity.ProductSize("Standard",
                                        basePrice, saved);
                        productSizeRepository.save(s);
                }
        }

        private void seedIngredients() {
                if (ingredientRepository.count() > 0) {
                        System.out.println("Ingredients already seeded.");
                        return;
                }

                // Common Coffee Shop Ingredients
                createIngredient("Coffee Beans", "g", 5000.0, 50.0);
                createIngredient("Espresso Shot", "shot", 2000.0, 8.0);
                createIngredient("Whole Milk", "ml", 10000.0, 20.0);
                createIngredient("Oat Milk", "ml", 5000.0, 35.0);
                createIngredient("Condensed Milk", "ml", 3000.0, 30.0);
                createIngredient("Heavy Cream", "ml", 2000.0, 45.0);
                createIngredient("Sugar Syrup", "ml", 5000.0, 15.0);
                createIngredient("Vanilla Syrup", "ml", 2000.0, 40.0);
                createIngredient("Caramel Syrup", "ml", 2000.0, 45.0);
                createIngredient("Hazelnut Syrup", "ml", 1500.0, 50.0);
                createIngredient("Green Tea Powder", "g", 1000.0, 200.0);
                createIngredient("Black Tea Leaves", "g", 2000.0, 80.0);
                createIngredient("Peach Puree", "g", 3000.0, 60.0);
                createIngredient("Strawberry Puree", "g", 3000.0, 70.0);
                createIngredient("Mango Puree", "g", 2500.0, 65.0);
                createIngredient("Tapioca Pearls (Boba)", "g", 5000.0, 40.0);
                createIngredient("Coconut Jelly", "g", 3000.0, 50.0);
                createIngredient("Whipped Cream", "ml", 2000.0, 55.0);
                createIngredient("Chocolate Powder", "g", 2000.0, 90.0);
                createIngredient("Ice", "g", 50000.0, 2.0);
                createIngredient("Water", "ml", 100000.0, 0.5);

                System.out.println("✓ Ingredients seeded successfully (21 items).");
        }

        private void createIngredient(String name, String unit, Double stock, Double cost) {
                Ingredient i = new Ingredient();
                i.setName(name);
                i.setUnit(unit);
                i.setStockQuantity(stock);
                i.setCostPerUnit(cost);
                ingredientRepository.save(i);
        }

        private void seedRecipes() {
                if (productRecipeRepository.count() > 0) {
                        System.out.println("Recipes already seeded.");
                        return;
                }

                // Cafe Latte Recipe
                createRecipe("Cafe Latte", "Espresso Shot", 1.0); // 1 Shot
                createRecipe("Cafe Latte", "Whole Milk", 200.0); // 200 ml
                createRecipe("Cafe Latte", "Sugar Syrup", 20.0); // 20 ml

                // Espresso Recipe
                createRecipe("Espresso", "Espresso Shot", 2.0); // 2 Shots (Double)

                // Peach Tea Recipe
                createRecipe("Peach Tea", "Black Tea Leaves", 5.0); // 5g tea
                createRecipe("Peach Tea", "Peach Puree", 40.0); // 40g puree
                createRecipe("Peach Tea", "Sugar Syrup", 30.0); // 30 ml
                createRecipe("Peach Tea", "Ice", 150.0); // 150g ice

                // Strawberry Smoothie Recipe
                createRecipe("Strawberry Smoothie", "Strawberry Puree", 60.0);
                createRecipe("Strawberry Smoothie", "Condensed Milk", 30.0);
                createRecipe("Strawberry Smoothie", "Whole Milk", 100.0);
                createRecipe("Strawberry Smoothie", "Ice", 200.0);

                // Coconut Juice Recipe
                createRecipe("Coconut Juice", "Water", 200.0); // Simple base
                createRecipe("Coconut Juice", "Coconut Jelly", 50.0);
                createRecipe("Coconut Juice", "Ice", 100.0);

                System.out.println("✓ Sample Recipes seeded successfully.");
        }

        private void createRecipe(String productName, String ingredientName, Double quantity) {
                productRepository.findAll().stream()
                                .filter(p -> p.getName().equalsIgnoreCase(productName))
                                .findFirst()
                                .ifPresent(product -> {
                                        ingredientRepository.findByName(ingredientName).ifPresent(ingredient -> {
                                                ProductRecipe recipe = new ProductRecipe();
                                                recipe.setProduct(product);
                                                recipe.setIngredient(ingredient);
                                                recipe.setQuantityRequired(quantity);
                                                productRecipeRepository.save(recipe);
                                        });
                                });
        }
}
