package br.com.fiap.postech.adjt.checkout.service;

import br.com.fiap.postech.adjt.checkout.dto.CheckoutRequestDTO;
import br.com.fiap.postech.adjt.checkout.dto.PaymentMethodFieldsRequestDTO;
import br.com.fiap.postech.adjt.checkout.dto.PaymentMethodRequestDTO;
import br.com.fiap.postech.adjt.checkout.mapper.PaymentMethodMapper;
import br.com.fiap.postech.adjt.checkout.model.*;
import br.com.fiap.postech.adjt.checkout.repository.CheckoutRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class CheckoutServiceTest {

    @InjectMocks
    private CheckoutService checkoutService;
    @Mock
    private OrderService orderService;
    @Mock
    private CheckoutRepository checkoutRepository;
    @Mock
    private CartService cartService;
    @Mock
    private PaymentProducer paymentProducer;
    @Mock
    private PaymentMethodMapper paymentMethodMapper;

    private UUID consumerId;
    private CheckoutRequestDTO checkoutRequestDTO;
    private PaymentMethodRequestDTO paymentMethodRequestDTO;
    private PaymentMethodFieldsRequestDTO paymentMethodFieldsRequestDTO;
    private Cart cart;
    @Mock
    private List<Item> itemList;
    private Item item;
    private UUID orderId;
    private Checkout checkout;
    private PaymentMethod paymentMethod;
    private PaymentMethodFields paymentMethodFields;
    private Order order;
    @Mock
    private List<Order> orderList;

    AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        checkoutService = new CheckoutService(orderService, checkoutRepository, cartService, paymentProducer);
        consumerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        paymentMethodFieldsRequestDTO = PaymentMethodFieldsRequestDTO.builder()
                .number("123")
                .expiration_month(LocalDate.now().getMonth().toString())
                .expiration_year(LocalDate.now().plusYears(1).toString())
                .cvv("456")
                .name("nome cliente").build();
        paymentMethodRequestDTO = PaymentMethodRequestDTO.builder()
                .type(PaymentMethodType.br_credit_card)
                .fields(paymentMethodFieldsRequestDTO).build();
        checkoutRequestDTO = CheckoutRequestDTO.builder()
                .consumerId(consumerId.toString())
                .amount(100.0)
                .currency(Currency.BRL.toString())
                .paymentMethod(paymentMethodRequestDTO).build();
        item = Item.builder()
                .itemId(1L)
                .quantity(1)
                .price(100.0).build();
        itemList.add(item);
        cart = Cart.builder()
                .consumerId(consumerId)
                .itemList(itemList).build();
        paymentMethodFields = PaymentMethodFields.builder()
                .number("123")
                .expiration_month(LocalDate.now().getMonth().toString())
                .expiration_year(LocalDate.now().plusYears(1).toString())
                .cvv("456")
                .name("nome cliente").build();
        paymentMethod = PaymentMethod.builder()
                .type(PaymentMethodType.br_credit_card)
                .fields(paymentMethodFields).build();
        checkout = Checkout.builder()
                .consumerId(consumerId)
                .orderId(orderId)
                .amount(100.0)
                .currency(Currency.BRL)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.pending).build();
        order = Order.builder()
                .orderId(orderId)
                .consumerId(consumerId)
                .itemList(itemList)
                .currency(Currency.BRL)
                .paymentMethodType(PaymentMethodType.br_credit_card)
                .totalValue(100.0)
                .paymentStatus(PaymentStatus.pending).build();
        orderList.add(order);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void deveProcessarPagamentoComSucesso() {

        when(cartService.getCart(UUID.fromString(checkoutRequestDTO.consumerId()))).thenReturn(cart);
        when(orderService.createAndSaveOrder(any(Cart.class), any(Checkout.class))).thenReturn(order);

        var response = checkoutService.processPayment(checkoutRequestDTO);

        verify(cartService, times(1)).getCart(UUID.fromString(checkoutRequestDTO.consumerId()));
        verify(checkoutRepository, times(1)).saveAndFlush(checkout);
        verify(orderService, times(1)).createAndSaveOrder(cart, checkout);
        verify(paymentProducer, times(1)).sendPaymentRequest(order, checkout);

    }

    @Test
    void deveProcessarPagamentoRetornandoException() {

        when(cartService.getCart(UUID.fromString(checkoutRequestDTO.consumerId()))).thenReturn(cart);
        when(orderService.createAndSaveOrder(any(Cart.class), any(Checkout.class))).thenThrow(IllegalArgumentException.class);

        Assertions.assertThrows(RuntimeException.class, () -> checkoutService.processPayment(checkoutRequestDTO));

    }

    @Test
    void deveBuscarPagamentoPeloOrderID() {

        when(orderService.getOrderByOrderId(orderId.toString())).thenReturn(order);

        var response = checkoutService.searchPaymentByOrderId(orderId.toString());

        verify(orderService, times(1)).getOrderByOrderId(orderId.toString());

    }

    @Test
    void deveBuscarPagamentoPeloConsumer() {

        when(orderService.getOrderByConsumerId(consumerId.toString())).thenReturn(orderList);

        var response = checkoutService.searchPaymentByConsumer(consumerId.toString());

        verify(orderService, times(1)).getOrderByConsumerId(consumerId.toString());

    }

}
