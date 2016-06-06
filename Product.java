public class Product {
		String code;
		int goodReads;
		int badReads;
		
		public Product(String code) {
			this.code = code;
		}
		
		public void setGoodReads(int i) {
			goodReads = goodReads + i;
		}
		
		public void incrementBadReads() {
			badReads++;
		}
		
		public int getGoodReads() {
			return goodReads;
		}
		
		public int getBadReads() {
			return badReads;
		}
		
		public void resetBadReads() {
			badReads = 0;
		}
	}