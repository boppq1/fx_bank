
public class msin {

	public static void main(String[] args) {
		String menuName = "치킨 세트";
		int price = 18000;
		int quantity = 3;
		boolean hasCoupon = false;
		
		int totalPrice = price*quantity;
		int deliveryFee;
		if(totalPrice >= 50000) {
			deliveryFee = 0;
		}else {
			deliveryFee = 3000;
		}
		int a = 2000;
		if(hasCoupon == true) {
			totalPrice -= a;
		}
		int finalAmount = totalPrice + deliveryFee;
		
		System.out.println("--- 주문 영수증 ---");
		System.out.println("주문메뉴: " + menuName);
		System.out.println("주문수량: " + quantity + "개");
		System.out.println("주문 금액: " + totalPrice);
		System.out.println("할인 금액: " + a);
		System.out.println("배달 비용: " + deliveryFee);
		System.out.println("--- 주문 영수증 ---");
		for(int i=0; i<3; i++) {
			System.out.println("배달 조리를 시작합니다.");
		}
		
	}
}
