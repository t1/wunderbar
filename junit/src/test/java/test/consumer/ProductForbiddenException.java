package test.consumer;

class ProductForbiddenException extends RuntimeException {
    public ProductForbiddenException(String id) {super("product " + id + " is forbidden");}
}
