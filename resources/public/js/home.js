$(document).ready(
  function() {
    $('#new-account').submit(
      function(e) {

        e.preventDefault();
        $('#success-message, #failure-message').hide();

        $.ajax({
          url: '/api/account',
          type: 'POST',
          data: {token: $('#token').val(),
                 key: $('#key').val()},
          success: function() {
            $('#success-message').css({display: 'inline-block'});
            $('#token, #key').val('');
          },
          error: function() {
            $('#failure-message').css({display: 'inline-block'});
          }
        });
      }
    );
  }
);
