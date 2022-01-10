package test.consumer;

class ProductNotFoundException extends RuntimeException {
    ProductNotFoundException(String id) {super("product " + id + " not found");}
}
