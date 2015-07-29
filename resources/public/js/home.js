$(document).ready(
  function() {
    $('#new-account').submit(function(e) {

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
    });

    $('#demo').submit(function(e) {
      e.preventDefault();

      $('#demo-meme').attr('src', '/images/loading.gif').show();

      $.ajax({
        url: '/api/meme',
        type: 'POST',
        data: {text: $('#demo-text').val()},
        success: function(r) {
          $('#demo-meme').attr('src', r);
        },
        error: function(r) {
          $('#demo-failure-message').html(r);
        }
      });
    });

  });
